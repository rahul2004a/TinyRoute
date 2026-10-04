import importlib.util
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("ci", Path(__file__).with_name("ci.py"))
ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ci)


class ChangesTest(unittest.TestCase):
    def test_application_and_shared_paths(self):
        self.assertEqual(ci.classify(["frontend/src/deleted.ts"]), (True, False))
        self.assertEqual(ci.classify(["backend/pom.xml"]), (False, True))
        for path in [".github/scripts/ci.py", "AGENTS.md", "DESIGN.md", "compose.yml", ".gitattributes",
                     ".env.example", "docs/requirements/Functional.md",
                     "docs/architecture/architecture.md", "docs/decisions/0002-backend-stack.md"]:
            with self.subTest(path=path):
                self.assertEqual(ci.classify([path]), (True, True))
        self.assertEqual(ci.classify(["Readme.md", "docs/spec/example/spec.md"]), (False, False))

    def test_git_diff_includes_deletions_and_both_sides_of_rename(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", "-C", directory, *args], text=True).strip()
            git("init", "-q")
            git("config", "user.name", "CI Test")
            git("config", "user.email", "ci@example.test")
            (repo / "frontend").mkdir()
            (repo / "frontend/removed.ts").write_text("removed")
            (repo / "frontend/moved.ts").write_text("moved")
            git("add", ".")
            git("commit", "-qm", "base")
            base = git("rev-parse", "HEAD")
            (repo / "frontend/removed.ts").unlink()
            (repo / "backend").mkdir()
            (repo / "frontend/moved.ts").rename(repo / "backend/moved.ts")
            git("add", ".")
            git("commit", "-qm", "head")
            self.assertEqual(ci.detect(base, "HEAD", "pull_request", repo), (True, True))
            self.assertEqual(ci.detect("0" * 40, "HEAD", "push", repo), (True, True))
            with self.assertRaises(subprocess.CalledProcessError):
                ci.detect("missing-ref", "HEAD", "push", repo)

    def test_pr_excludes_unrelated_changes_on_base_branch(self):
        with tempfile.TemporaryDirectory() as directory:
            repo = Path(directory)
            def git(*args):
                return subprocess.check_output(["git", "-C", directory, *args], text=True).strip()
            git("init", "-q")
            git("config", "user.name", "CI Test")
            git("config", "user.email", "ci@example.test")
            (repo / "Readme.md").write_text("base")
            git("add", ".")
            git("commit", "-qm", "base")
            base = git("rev-parse", "HEAD")
            git("switch", "-qc", "pr")
            (repo / "Readme.md").write_text("PR documentation")
            git("commit", "-qam", "PR")
            head = git("rev-parse", "HEAD")
            git("switch", "-qc", "base-changed", base)
            (repo / "backend").mkdir()
            (repo / "backend/pom.xml").write_text("unrelated backend change")
            git("add", ".")
            git("commit", "-qm", "base advances")
            self.assertEqual(ci.detect("HEAD", head, "pull_request", repo), (False, False))
            self.assertEqual(ci.detect(base, "HEAD", "push", repo), (False, True))


class GateTest(unittest.TestCase):
    def needs(self, changed="true", precheck="success", verify="success"):
        return {"precheck": {"result": precheck, "outputs": {"frontend": changed}},
                "verify": {"result": verify}}

    def test_affected_application_requires_success(self):
        self.assertTrue(ci.gate(self.needs(), "frontend", ["verify"]))
        for result in ["failure", "cancelled", "skipped", "unknown"]:
            self.assertFalse(ci.gate(self.needs(verify=result), "frontend", ["verify"]))

    def test_unaffected_passes_only_after_successful_precheck(self):
        self.assertTrue(ci.gate(self.needs(changed="false", verify="skipped"), "frontend", ["verify"]))
        for result in ["failure", "cancelled", "skipped"]:
            self.assertFalse(ci.gate(self.needs(changed="false", precheck=result), "frontend", ["verify"]))
        for missing in [{}, self.needs(changed=""), self.needs(changed="invalid")]:
            self.assertFalse(ci.gate(missing, "frontend", ["verify"]))

    def test_unconditional_checks_cannot_be_skipped(self):
        needs = self.needs(changed="false", verify="skipped")
        needs["secrets"] = {"result": "success"}
        self.assertTrue(ci.gate(needs, "frontend", ["verify"], ["secrets"]))
        needs["secrets"]["result"] = "failure"
        self.assertFalse(ci.gate(needs, "frontend", ["verify"], ["secrets"]))


class SarifTest(unittest.TestCase):
    def test_diff_informed_reports_cannot_pass_the_full_backend_gate(self):
        with tempfile.TemporaryDirectory() as directory:
            self.write_report(directory, results=False)
            path = Path(directory, "java.sarif")
            report = json.loads(path.read_text())
            for mode in ["diff-informed", "overlay,diff-informed"]:
                with self.subTest(mode=mode):
                    report["runs"][0]["properties"] = {"incrementalMode": mode}
                    path.write_text(json.dumps(report))
                    with self.assertRaisesRegex(ValueError, "full backend"):
                        ci.sarif_findings(Path(directory))
            report["runs"][0]["properties"] = {}
            path.write_text(json.dumps(report))
            self.assertEqual(ci.sarif_findings(Path(directory)), [])

    def write_report(self, directory, score="7.0", level="warning", results=True):
        report = {"version": "2.1.0", "runs": [{
            "tool": {"driver": {"rules": [{"id": "java/test", "properties": {"security-severity": score}}]}},
            "results": [{"ruleId": "java/test", "level": level}] if results else []}]}
        Path(directory, "java.sarif").write_text(json.dumps(report))

    def test_security_threshold_and_errors(self):
        with tempfile.TemporaryDirectory() as directory:
            for score, level, blocking in [("7.0", "warning", True), ("9.8", "warning", True),
                                            ("6.9", "warning", False), ("0", "error", True)]:
                self.write_report(directory, score, level)
                self.assertEqual(bool(ci.sarif_findings(Path(directory))), blocking)
            self.write_report(directory, results=False)
            self.assertEqual(ci.sarif_findings(Path(directory)), [])

    def test_missing_invalid_and_unrecognized_results_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                ci.sarif_findings(Path(directory))
            for report in [{}, {"runs": []}, {"runs": [{"results": [{}]}]}]:
                Path(directory, "java.sarif").write_text(json.dumps(report))
                with self.assertRaises(ValueError):
                    ci.sarif_findings(Path(directory))

    def test_codeql_extension_rule_reference_and_default_level(self):
        with tempfile.TemporaryDirectory() as directory:
            rule = {"id": "java/log-injection", "properties": {"security-severity": "6.1"},
                    "defaultConfiguration": {"level": "warning"}}
            result = {"ruleId": rule["id"],
                      "rule": {"id": rule["id"], "index": 0, "toolComponent": {"index": 1}}}
            report = {"version": "2.1.0", "runs": [{"tool": {
                "driver": {"rules": []}, "extensions": [{"rules": []}, {"rules": [rule]}]},
                "results": [result]}]}
            path = Path(directory, "java.sarif")
            path.write_text(json.dumps(report))
            self.assertEqual(ci.sarif_findings(Path(directory)), [])
            rule["properties"]["security-severity"] = "7.0"
            path.write_text(json.dumps(report))
            self.assertEqual(ci.sarif_findings(Path(directory)), [rule["id"]])
            rule["properties"]["security-severity"] = "6.1"
            rule["defaultConfiguration"]["level"] = "error"
            path.write_text(json.dumps(report))
            self.assertEqual(ci.sarif_findings(Path(directory)), [rule["id"]])
            result["rule"]["index"] = -1
            path.write_text(json.dumps(report))
            with self.assertRaises(ValueError):
                ci.sarif_findings(Path(directory))


class ReportCompletenessTest(unittest.TestCase):
    def test_coverage_requires_data_and_meets_threshold(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            with self.assertRaises(ValueError):
                ci.coverage(target)
            (target / "jacoco.exec").write_bytes(b"execution-data")
            (target / "site/jacoco").mkdir(parents=True)
            report = target / "site/jacoco/jacoco.xml"
            for xml, passed in [
                ('<report><counter type="LINE" covered="70" missed="30"/></report>', True),
                ('<report><counter type="LINE" covered="69" missed="31"/></report>', False),
                ('<report><counter type="LINE" covered="0" missed="0"/></report>', False),
                ('<report/>', False),
            ]:
                report.write_text(xml)
                if passed:
                    self.assertEqual(ci.coverage(target), 0.7)
                else:
                    with self.assertRaises(ValueError):
                        ci.coverage(target)

    def test_trivy_requires_java_packages_and_image_os_inventory(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory, "trivy.json")
            java = {"Class": "lang-pkgs", "Type": "jar",
                    "Packages": [{"Name": "org.springframework.boot:spring-boot", "Version": "4.1.1"}]}
            os = {"Class": "os-pkgs", "Type": "ubuntu",
                  "Packages": [{"Name": "libc6", "Version": "2.39"}]}
            for results in [[], [{}], [os]]:
                report.write_text(json.dumps({"Results": results}))
                with self.assertRaises(ValueError):
                    ci.trivy_inventory(report, False)
            report.write_text(json.dumps({"Results": [java]}))
            ci.trivy_inventory(report, False)
            with self.assertRaises(ValueError):
                ci.trivy_inventory(report, True)
            report.write_text(json.dumps({"Results": [java, os]}))
            ci.trivy_inventory(report, True)


if __name__ == "__main__":
    unittest.main()
