"""Installer security regression tests with attacker-controlled release assets."""

import hashlib
import io
import os
from pathlib import Path
import subprocess
import sys
import tarfile
import tempfile
import unittest


INSTALLER = Path(__file__).with_name("install-tool.sh").resolve()


class ToolInstallerTest(unittest.TestCase):
    def test_replaced_archive_and_matching_remote_checksum_are_rejected(self):
        for system, architecture in (("Linux", "x86_64"), ("Darwin", "arm64")):
            for tool in ("gitleaks", "trivy", "actionlint"):
                with self.subTest(platform=f"{system}/{architecture}", tool=tool):
                    with tempfile.TemporaryDirectory() as directory:
                        root = Path(directory)
                        mock_bin = root / "bin"
                        mock_bin.mkdir()
                        archive = root / "replacement.tar.gz"
                        payload = b'#!/usr/bin/env bash\nprintf executed > "$INSTALLER_CANARY"\n'
                        with tarfile.open(archive, "w:gz") as output:
                            member = tarfile.TarInfo(tool)
                            member.size = len(payload)
                            member.mode = 0o755
                            output.addfile(member, io.BytesIO(payload))
                        digest = hashlib.sha256(archive.read_bytes()).hexdigest()
                        uname = mock_bin / "uname"
                        uname.write_text(
                            "#!/usr/bin/env bash\n"
                            f'if [[ "$1" == "-s" ]]; then echo {system}; '
                            f"else echo {architecture}; fi\n"
                        )
                        curl = mock_bin / "curl"
                        curl.write_text(
                            f"#!{sys.executable}\n"
                            "import os, pathlib, shutil, sys\n"
                            "args = sys.argv[1:]\n"
                            "url = next(arg for arg in args if arg.startswith('https://'))\n"
                            "destination = pathlib.Path(args[args.index('-o') + 1])\n"
                            "if url.endswith('.tar.gz'):\n"
                            "    shutil.copyfile(os.environ['REPLACEMENT_ARCHIVE'], destination)\n"
                            "else:\n"
                            "    destination.write_text(os.environ['REPLACEMENT_CHECKSUM'])\n"
                        )
                        uname.chmod(0o755)
                        curl.chmod(0o755)
                        platforms = {
                            ("gitleaks", "Linux"): "linux_x64",
                            ("gitleaks", "Darwin"): "darwin_arm64",
                            ("trivy", "Linux"): "Linux-64bit",
                            ("trivy", "Darwin"): "macOS-ARM64",
                            ("actionlint", "Linux"): "linux_amd64",
                            ("actionlint", "Darwin"): "darwin_arm64",
                        }
                        versions = {"gitleaks": "8.30.1", "trivy": "0.75.0", "actionlint": "1.7.12"}
                        name = f"{tool}_{versions[tool]}_{platforms[(tool, system)]}.tar.gz"
                        destination = root / "installed"
                        canary = root / "executed"
                        environment = os.environ.copy()
                        environment.update(
                            PATH=f"{mock_bin}{os.pathsep}{environment['PATH']}",
                            REPLACEMENT_ARCHIVE=str(archive),
                            REPLACEMENT_CHECKSUM=f"{digest}  {name}\n",
                            INSTALLER_CANARY=str(canary),
                            GITHUB_PATH="",
                        )
                        result = subprocess.run(
                            ["bash", str(INSTALLER), tool, str(destination)],
                            env=environment,
                            capture_output=True,
                            text=True,
                            timeout=10,
                        )
                        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
                        self.assertIn("Checksum verification failed", result.stderr)
                        self.assertFalse((destination / tool).exists())
                        self.assertFalse(canary.exists(), "Replaced binary was executed")


if __name__ == "__main__":
    unittest.main()
