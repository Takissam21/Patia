import csv
import os
import re
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent

RESULTS_DIR = ROOT / "results"
RAW_DIR = RESULTS_DIR / "raw_outputs"
FIGURES_DIR = RESULTS_DIR / "figures"

TIMEOUT_SECONDS = 180

DOMAINS = {
    "blocksworld": {
        "dir": ROOT / "benchmarks" / "blocks",
        "problems": [
            "p001.pddl",
            "p002.pddl",
            "p003.pddl",
            "p004.pddl",
            "p005.pddl",
            "p006.pddl",
            "p007.pddl",
            "p008.pddl",
        ],
    },
    "logistics": {
        "dir": ROOT / "benchmarks" / "logistics" / "strips-typed",
        "problems": [
            "p01.pddl",
            "p02.pddl",
            "p03.pddl",
            "p04.pddl",
            "p05.pddl",
            "p06.pddl",
            "p07.pddl",
            "p08.pddl",
        ],
    },
    "taquin": {
        "dir": ROOT / "benchmarks" / "taquin",
        "problems": [
            "p01.pddl",
            "p02.pddl",
            "p03.pddl",
            "p04.pddl",
            "p05.pddl",
        ],
    },
}


def prepare_dirs():
    RESULTS_DIR.mkdir(exist_ok=True)
    RAW_DIR.mkdir(exist_ok=True)
    FIGURES_DIR.mkdir(exist_ok=True)

    for old_file in RAW_DIR.glob("*.txt"):
        old_file.unlink()

    for old_file in FIGURES_DIR.glob("*.png"):
        old_file.unlink()


def compile_satplanner():
    print("Compilation du SATPlanner...")

    classes_dir = ROOT / "classes"
    classes_dir.mkdir(exist_ok=True)

    classpath = os.pathsep.join([
        str(ROOT / "lib" / "pddl4j-4.0.0.jar"),
        str(ROOT / "lib" / "org.sat4j.core.jar"),
        str(ROOT / "lib" / "sat4j-sat.jar"),
    ])

    cmd = [
        "javac",
        "-d", str(classes_dir),
        "-cp", classpath,
        str(ROOT / "src" / "fr" / "uga" / "pddl4j" / "yasp" / "SATEncoding.java"),
        str(ROOT / "src" / "fr" / "uga" / "pddl4j" / "yasp" / "YetAnotherSATPlanner.java"),
    ]

    result = subprocess.run(
        cmd,
        cwd=ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
    )

    if result.returncode != 0:
        print(result.stdout)
        raise RuntimeError("Erreur pendant la compilation.")

    print("Compilation réussie.\n")


def count_plan_actions(output):
    count = 0

    for line in output.splitlines():
        if re.match(r"^\s*\d+\s*:\s*\(", line):
            count += 1

    return count


def run_command(cmd, output_file):
    start = time.perf_counter()

    try:
        result = subprocess.run(
            cmd,
            cwd=ROOT,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=TIMEOUT_SECONDS,
        )

        elapsed = time.perf_counter() - start
        output = result.stdout
        return_code = result.returncode
        timeout = False

    except subprocess.TimeoutExpired as error:
        elapsed = time.perf_counter() - start
        output = error.stdout or ""

        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")

        output += f"\nTIMEOUT after {TIMEOUT_SECONDS} seconds\n"
        return_code = -1
        timeout = True

    output_file.write_text(output, encoding="utf-8", errors="replace")

    plan_length = count_plan_actions(output)

    if timeout:
        status = "TIMEOUT"
    elif plan_length > 0:
        status = "SOLVED"
    else:
        status = "FAILED"

    return {
        "status": status,
        "time_seconds": elapsed,
        "plan_length": plan_length,
        "return_code": return_code,
        "output_file": str(output_file.relative_to(ROOT)),
    }


def run_hsp(domain_name, problem_name, domain_file, problem_file):
    output_file = RAW_DIR / f"{domain_name}_{problem_name}_hsp.txt"

    cmd = [
        "java",
        "-cp", str(ROOT / "lib" / "pddl4j-4.0.0.jar"),
        "fr.uga.pddl4j.planners.statespace.HSP",
        str(domain_file),
        str(problem_file),
    ]

    return run_command(cmd, output_file)


def run_satplanner(domain_name, problem_name, domain_file, problem_file):
    output_file = RAW_DIR / f"{domain_name}_{problem_name}_satplanner.txt"

    classpath = os.pathsep.join([
        str(ROOT / "classes"),
        str(ROOT / "lib" / "pddl4j-4.0.0.jar"),
        str(ROOT / "lib" / "org.sat4j.core.jar"),
        str(ROOT / "lib" / "sat4j-sat.jar"),
    ])

    cmd = [
        "java",
        "-server",
        "-Xms2048m",
        "-Xmx2048m",
        "-cp", classpath,
        "fr.uga.pddl4j.yasp.YetAnotherSATPlanner",
        str(domain_file),
        str(problem_file),
    ]

    return run_command(cmd, output_file)


def run_benchmarks():
    rows = []

    for domain_name, config in DOMAINS.items():
        domain_dir = config["dir"]
        domain_file = domain_dir / "domain.pddl"

        print(f"\n=== Domaine : {domain_name} ===")

        if not domain_file.exists():
            print(f"Domaine introuvable : {domain_file}")
            continue

        for problem in config["problems"]:
            problem_file = domain_dir / problem
            problem_name = Path(problem).stem

            if not problem_file.exists():
                print(f"Problème introuvable : {problem_file}")
                continue

            print(f"\nProblème : {problem}")

            print("  Lancement HSP...")
            hsp_result = run_hsp(domain_name, problem_name, domain_file, problem_file)
            print(
                f"  HSP : {hsp_result['status']} | "
                f"temps = {hsp_result['time_seconds']:.3f}s | "
                f"longueur = {hsp_result['plan_length']}"
            )

            rows.append({
                "domain": domain_name,
                "problem": problem_name,
                "planner": "HSP",
                **hsp_result,
            })

            print("  Lancement SATPlanner...")
            sat_result = run_satplanner(domain_name, problem_name, domain_file, problem_file)
            print(
                f"  SATPlanner : {sat_result['status']} | "
                f"temps = {sat_result['time_seconds']:.3f}s | "
                f"longueur = {sat_result['plan_length']}"
            )

            rows.append({
                "domain": domain_name,
                "problem": problem_name,
                "planner": "SATPlanner",
                **sat_result,
            })

    return rows


def save_csv(rows):
    csv_file = RESULTS_DIR / "benchmark_results.csv"

    fieldnames = [
        "domain",
        "problem",
        "planner",
        "status",
        "time_seconds",
        "plan_length",
        "return_code",
        "output_file",
    ]

    with csv_file.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"\nCSV écrit dans : {csv_file}")


def make_figures(rows):
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print("\nMatplotlib n'est pas installé.")
        print("Le CSV est généré, mais pas les graphes.")
        return

    for domain_name in DOMAINS.keys():
        domain_rows = [r for r in rows if r["domain"] == domain_name]
        hsp_rows = [r for r in domain_rows if r["planner"] == "HSP"]

        hsp_rows = sorted(hsp_rows, key=lambda r: float(r["time_seconds"]))
        problems = [r["problem"] for r in hsp_rows]

        def value(planner, problem, key):
            for r in domain_rows:
                if r["planner"] == planner and r["problem"] == problem:
                    if r["status"] == "SOLVED":
                        return float(r[key])
                    return None
            return None

        x = list(range(len(problems)))

        hsp_times = [value("HSP", p, "time_seconds") for p in problems]
        sat_times = [value("SATPlanner", p, "time_seconds") for p in problems]

        plt.figure()
        plt.plot(x, hsp_times, marker="o", label="HSP")
        plt.plot(x, sat_times, marker="o", label="SATPlanner")
        plt.xticks(x, problems, rotation=45)
        plt.xlabel("Problèmes triés selon le temps HSP")
        plt.ylabel("Temps total mesuré en secondes")
        plt.title(f"{domain_name} - temps")
        plt.legend()
        plt.tight_layout()
        plt.savefig(FIGURES_DIR / f"{domain_name}_time.png")
        plt.close()

        hsp_lengths = [value("HSP", p, "plan_length") for p in problems]
        sat_lengths = [value("SATPlanner", p, "plan_length") for p in problems]

        plt.figure()
        plt.plot(x, hsp_lengths, marker="o", label="HSP")
        plt.plot(x, sat_lengths, marker="o", label="SATPlanner")
        plt.xticks(x, problems, rotation=45)
        plt.xlabel("Problèmes triés selon le temps HSP")
        plt.ylabel("Longueur du plan")
        plt.title(f"{domain_name} - longueur du plan")
        plt.legend()
        plt.tight_layout()
        plt.savefig(FIGURES_DIR / f"{domain_name}_length.png")
        plt.close()

    print(f"Graphes écrits dans : {FIGURES_DIR}")


def main():
    prepare_dirs()
    compile_satplanner()

    rows = run_benchmarks()

    save_csv(rows)
    make_figures(rows)

    print("\nBenchmark terminé.")
    print(f"Résultats : {RESULTS_DIR / 'benchmark_results.csv'}")
    print(f"Graphes   : {FIGURES_DIR}")


if __name__ == "__main__":
    main()