import subprocess
import tempfile
import unittest
from pathlib import Path

from tools.validate_documentation import Check, check_evidence


class ReleaseEvidenceBindingTest(unittest.TestCase):

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        subprocess.run(['git', 'init', '-q'], cwd=self.root, check=True)
        subprocess.run(['git', 'config', 'user.email', 'test@example.invalid'], cwd=self.root, check=True)
        subprocess.run(['git', 'config', 'user.name', 'Evidence Test'], cwd=self.root, check=True)
        (self.root/'maintainer').mkdir()
        (self.root/'source.txt').write_text('qualified\n', encoding='utf-8')
        self._commit('qualified source')
        self.qualified = self._git('rev-parse', 'HEAD')
        self._write_evidence(self.qualified)
        self._commit('record evidence')

    def tearDown(self):
        self.temporary.cleanup()

    def test_current_gate_allows_only_the_evidence_record_commit(self):
        check = Check()
        check_evidence(self.root, check)
        self.assertEqual([], check.errors)

    def test_current_gate_rejects_later_source_commit(self):
        (self.root/'source.txt').write_text('changed after qualification\n', encoding='utf-8')
        self._commit('change source')
        check = Check()
        check_evidence(self.root, check)
        self.assertTrue(any('single evidence-only' in error for error in check.errors))

    def test_current_gate_rejects_a_second_evidence_only_commit(self):
        self._write_evidence(self.qualified)
        with (self.root/'maintainer/release-evidence.md').open('a', encoding='utf-8') as evidence:
            evidence.write('\nLater ledger edit.\n')
        self._commit('edit evidence again')
        check = Check()
        check_evidence(self.root, check)
        self.assertTrue(any('single evidence-only' in error for error in check.errors))

    def test_current_gate_rejects_dirty_worktree(self):
        (self.root/'source.txt').write_text('dirty\n', encoding='utf-8')
        check = Check()
        check_evidence(self.root, check)
        self.assertTrue(any('clean working tree' in error for error in check.errors))

    def test_historical_mode_checks_ledger_without_matching_head(self):
        (self.root/'source.txt').write_text('later\n', encoding='utf-8')
        self._commit('later source')
        check = Check()
        check_evidence(self.root, check, historical=True)
        self.assertEqual([], check.errors)

    def _write_evidence(self, revision):
        (self.root/'maintainer/release-evidence.md').write_text(
            '# Evidence\n\nQualified source revision: `' + revision + '`\n',
            encoding='utf-8',
        )

    def _commit(self, message):
        subprocess.run(['git', 'add', '.'], cwd=self.root, check=True)
        subprocess.run(['git', 'commit', '-qm', message], cwd=self.root, check=True)

    def _git(self, *args):
        return subprocess.run(
            ['git', *args], cwd=self.root, check=True, capture_output=True, text=True
        ).stdout.strip()


if __name__ == '__main__':
    unittest.main()
