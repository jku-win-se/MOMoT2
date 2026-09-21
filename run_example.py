import os
import sys
import subprocess

def run_example(example_module_dir, main_class, extra_args=None):
    repo = os.path.abspath(os.path.dirname(__file__))
    example_dir = os.path.join(repo, example_module_dir)
    
    cp_entries = [
        os.path.join(repo, "plugins", "at.ac.tuwien.big.moea", "target", "classes"),
        os.path.join(repo, "plugins", "at.ac.tuwien.big.momot.core", "target", "classes"),
        os.path.join(repo, "plugins", "at.ac.tuwien.big.momot.lang", "target", "classes"),
        os.path.join(repo, "plugins", "at.ac.tuwien.big.momot.lang.ide", "target", "classes"),
        os.path.join(example_dir, "target", "classes"),
    ]
    
    # Add any extra example submodules target classes if present
    for root, dirs, files in os.walk(os.path.join(repo, "examples")):
        if "target" in dirs:
            classes_dir = os.path.join(root, "target", "classes")
            if os.path.isdir(classes_dir) and classes_dir not in cp_entries:
                cp_entries.append(classes_dir)

    # Add all lib/*.jar in plugins/at.ac.tuwien.big.moea/lib
    moea_lib = os.path.join(repo, "plugins", "at.ac.tuwien.big.moea", "lib")
    for f in os.listdir(moea_lib):
        if f.endswith(".jar"):
            cp_entries.append(os.path.join(moea_lib, f))

    # Add all lib/*.jar inside example projects (e.g., ecore/lib)
    for root, dirs, files in os.walk(os.path.join(repo, "examples")):
        if "lib" in dirs:
            lib_dir = os.path.join(root, "lib")
            for f in os.listdir(lib_dir):
                if f.endswith(".jar"):
                    cp_entries.append(os.path.join(lib_dir, f))

    # Add tycho cached jars
    tycho_cache = os.path.expanduser(r"~\.m2\repository\.cache\tycho")
    for root, dirs, files in os.walk(tycho_cache):
        for f in files:
            if f.endswith(".jar") and not f.endswith("-sources.jar") and not f.endswith("-javadoc.jar"):
                if "MOEAFramework-2.12" in f:
                    continue
                cp_entries.append(os.path.join(root, f))

    # Also add tools libs if present
    tools_dir = os.path.join(repo, "tools")
    if os.path.isdir(tools_dir):
        for root, dirs, files in os.walk(tools_dir):
            for f in files:
                if f.endswith(".jar"):
                    cp_entries.append(os.path.join(root, f))

    # Convert to forward slashes for java argfile
    cp_string = ";".join([os.path.abspath(p).replace("\\", "/") for p in cp_entries])
    
    # Write cp.args for java @cp.args with forward slashes and enclosed in quotes
    argfile_path = os.path.join(example_dir, "cp.args")
    with open(argfile_path, "w", encoding="utf-8") as f:
        f.write("-classpath\n")
        f.write(f'"{cp_string}"\n')

    cmd = [
        "java",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.text=ALL-UNNAMED",
        "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens", "java.scripting/javax.script=ALL-UNNAMED",
        f"@{argfile_path}",
        main_class
    ]
    if extra_args:
        cmd.extend(extra_args)

    print(f"Executing in cwd={example_dir}:")
    print(f"java ... {main_class}")
    
    result = subprocess.run(cmd, cwd=example_dir, text=True)
    print(f"Finished with exit code: {result.returncode}")
    return result.returncode

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python run_example.py <example_module_dir> <main_class> [extra args...]")
        sys.exit(1)
    
    module_dir = sys.argv[1]
    main_cls = sys.argv[2]
    args = sys.argv[3:] if len(sys.argv) > 3 else None
    
    code = run_example(module_dir, main_cls, args)
    sys.exit(code)
