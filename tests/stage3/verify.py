"""Run from the repo root: python3 tests/stage3/verify.py (JDK 17+)."""
from pathlib import Path
import subprocess


def run(args, **kwargs):
    return subprocess.run(args, text=True, capture_output=True, check=True, **kwargs).stdout


sources = [str(p) for p in Path('src').rglob('*.java')]
print(run(['javac', '--release', '17', '-d', 'out', *sources,
           'tests/stage3/Stage3RulesTest.java']), end='')
print(run(['java', '-cp', 'out', 'restaurant.entity.Stage3RulesTest']), end='')
for file in sorted(Path('tests/stage3').glob('*.txt')):
    output = run(['java', '-Decho=true', '-cp', 'out', 'restaurant.Main'], input=file.read_text())
    assert '[Input ended.' not in output, file
    assert 'Turn 18/18' in output, file
    result = output[output.rfind('Target                   You got'):]
    if 'defeat' in file.name:
        assert '*** DEFEAT ***' in output
        assert '0           MISSED' in result and '$0.00' in result and 'N/A' in result
        assert 'Customers who left unhappy: 2' in result
    else:
        assert '*** VICTORY ***' in output
        assert '5           MET' in result and '$86.16' in result and '87.8' in result
        assert 'Customers who left unhappy: 0' in result
        assert 'Salad + Burger Combo' in output
        assert '2 turn(s) remaining' in output and '1 turn(s) remaining' in output
        assert 'used / expired' in output
        if 'invalid' in file.name:
            assert 'is not a number' in output and 'Please type a number' in output
            assert "task has not been used" in " ".join(output.split())
    print(file.name + ': passed')
