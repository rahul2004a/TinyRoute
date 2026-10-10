"""Credential-free change detection and fail-closed CI gates."""

import argparse
import json
import math
import os
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path


def classify(paths):
    shared_files = {"AGENTS.md", "DESIGN.md", "compose.yml", ".env.example", ".gitattributes"}
    shared_prefixes = (".github/", "docs/requirements/", "docs/architecture/", "docs/decisions/")
    shared = any(path in shared_files or path.startswith(shared_prefixes) for path in paths)
    frontend_verification = {"tools/link-performance.mjs", "tools/link-performance.test.mjs"}
    return (shared or any(path.startswith("frontend/") or path in frontend_verification for path in paths),
            shared or any(path.startswith("backend/") for path in paths))


def detect(base, head, event, repo=Path(".")):
    if event == "push" and (not base or base == "0" * 40):
        return True, True
    if not base or not head:
        raise ValueError("Missing base/head revision")
    if event == "pull_request":
        base = subprocess.check_output(
            ["git", "merge-base", base, head], cwd=repo, text=True).strip()
    elif event != "push":
        raise ValueError(f"Unsupported event: {event}")
    output = subprocess.check_output(
        ["git", "diff", "--name-only", "--no-renames", "-z", base, head, "--"], cwd=repo)
    return classify(output.decode("utf-8", errors="surrogateescape").rstrip("\0").split("\0"))


def gate(needs, application, required, unconditional=()):
    precheck = needs.get("precheck", {})
    affected = precheck.get("outputs", {}).get(application)
    if precheck.get("result") != "success" or affected not in ("true", "false"):
        return False
    jobs = [*unconditional, *(required if affected == "true" else [])]
    return all(needs.get(job, {}).get("result") == "success" for job in jobs)


def sarif_rule(tool, result):
    reference = result.get("rule", {})
    rule_id = result.get("ruleId", reference.get("id"))
    if not rule_id or (reference.get("id") and reference["id"] != rule_id):
        raise ValueError("Missing or conflicting SARIF rule ID")
    component = tool.get("driver", {})
    component_reference = reference.get("toolComponent", {})
    if component_reference:
        extensions = tool.get("extensions", [])
        index = component_reference.get("index")
        if type(index) is not int or not 0 <= index < len(extensions):
            raise ValueError("Invalid SARIF tool component reference")
        component = extensions[index]
    rules = component.get("rules")
    if not isinstance(rules, list):
        raise ValueError("Missing SARIF component rules")
    index = reference.get("index", result.get("ruleIndex"))
    if index is not None:
        if type(index) is not int or not 0 <= index < len(rules):
            raise ValueError("Invalid SARIF rule index")
        rule = rules[index]
        if rule.get("id") != rule_id:
            raise ValueError("Conflicting SARIF rule index and ID")
        return rule
    matches = [rule for rule in rules if rule.get("id") == rule_id]
    if len(matches) != 1:
        raise ValueError("Unknown or ambiguous SARIF rule")
    return matches[0]


def sarif_findings(directory):
    files = sorted(directory.glob("*.sarif"))
    if not files:
        raise ValueError("No CodeQL SARIF reports produced")
    blocking = []
    for file in files:
        report = json.loads(file.read_text())
        if report.get("version") != "2.1.0" or not report.get("runs"):
            raise ValueError(f"Invalid SARIF report: {file}")
        for run in report["runs"]:
            modes = run.get("properties", {}).get("incrementalMode", "").split(",")
            if "diff-informed" in modes:
                raise ValueError("CodeQL must analyze the full backend, not only PR changes")
            tool = run.get("tool", {})
            results = run.get("results")
            if not isinstance(tool.get("driver", {}).get("rules"), list) or not isinstance(results, list):
                raise ValueError(f"Missing rules/results in {file}")
            for result in results:
                rule = sarif_rule(tool, result)
                rule_id = rule["id"]
                score = float(rule.get("properties", {}).get("security-severity", 0))
                if not math.isfinite(score) or not 0 <= score <= 10:
                    raise ValueError(f"Invalid security severity for {rule_id}")
                level = result.get("level", rule.get("defaultConfiguration", {}).get("level", "warning"))
                if score >= 7 or level == "error":
                    blocking.append(rule_id)
    return blocking


def coverage(target):
    execution = target / "jacoco.exec"
    report = target / "site/jacoco/jacoco.xml"
    if not execution.is_file() or execution.stat().st_size == 0 or not report.is_file():
        raise ValueError("Missing or empty JaCoCo execution data/report")
    root = ET.parse(report).getroot()
    counters = [c for c in root.findall("counter") if c.get("type") == "LINE"]
    if len(counters) != 1:
        raise ValueError("Missing application LINE coverage counter")
    covered, missed = int(counters[0].get("covered")), int(counters[0].get("missed"))
    if covered < 0 or missed < 0 or covered + missed == 0:
        raise ValueError("Invalid application LINE coverage counter")
    ratio = covered / (covered + missed)
    if ratio < 0.70:
        raise ValueError(f"Application line coverage {ratio:.2%} is below 70%")
    return ratio


def trivy_inventory(report_path, image):
    report = json.loads(report_path.read_text())
    results = report.get("Results", [])
    java = any(r.get("Type") == "jar" and r.get("Packages") for r in results)
    os_packages = any(r.get("Class") == "os-pkgs" and r.get("Packages") for r in results)
    if not java or (image and not os_packages):
        raise ValueError("Trivy report is missing the required Java/OS package inventory")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("changes")
    status = sub.add_parser("gate")
    status.add_argument("application", choices=["frontend", "backend"])
    status.add_argument("required", nargs="+")
    status.add_argument("--unconditional", nargs="*", default=[])
    sarif = sub.add_parser("sarif")
    sarif.add_argument("directory", type=Path)
    coverage_parser = sub.add_parser("coverage")
    coverage_parser.add_argument("target", type=Path)
    inventory = sub.add_parser("trivy-inventory")
    inventory.add_argument("report", type=Path)
    inventory.add_argument("--image", action="store_true")
    args = parser.parse_args()
    if args.command == "changes":
        frontend, backend = detect(os.environ.get("BASE_SHA"), os.environ.get("HEAD_SHA"),
                                   os.environ["EVENT_NAME"])
        outputs = f"frontend={str(frontend).lower()}\nbackend={str(backend).lower()}\n"
        with open(os.environ["GITHUB_OUTPUT"], "a") as file:
            file.write(outputs)
        print(outputs, end="")
    elif args.command == "gate":
        needs = json.loads(os.environ["NEEDS_JSON"])
        if not gate(needs, args.application, args.required, args.unconditional):
            raise SystemExit(f"{args.application} CI failed: {json.dumps(needs, sort_keys=True)}")
        print(f"{args.application} CI passed")
    elif args.command == "coverage":
        print(f"Application line coverage: {coverage(args.target):.2%}")
    elif args.command == "trivy-inventory":
        trivy_inventory(args.report, args.image)
        print("Trivy package inventory verified")
    else:
        findings = sarif_findings(args.directory)
        if findings:
            raise SystemExit(f"Blocking CodeQL findings: {', '.join(findings)}")
        print("No blocking CodeQL findings")


if __name__ == "__main__":
    main()
