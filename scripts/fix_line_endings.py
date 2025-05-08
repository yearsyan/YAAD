import os
import subprocess

WHITELISTED_EXTENSIONS = {'.java', '.kt', '.xml', '.gradle', '.properties', '.md', '.txt', '.gitignore', ".py"}
EXCLUDE_DIRS = {'.git', 'build', '.gradle', '.idea'}

def normalize_line_endings(file_path):
    with open(file_path, 'rb') as f:
        content = f.read()

    new_content = content.replace(b'\r\n', b'\n')

    if new_content != content:
        with open(file_path, 'wb') as f:
            f.write(new_content)
        print(f"Fixed LF: {file_path}")

def is_text_file(filename):
    return os.path.splitext(filename)[1].lower() in WHITELISTED_EXTENSIONS

def should_exclude_dir(dirname):
    return any(part in EXCLUDE_DIRS for part in dirname.split(os.sep))

print("Scanning files to normalize line endings...")

for root, dirs, files in os.walk('.'):
    if should_exclude_dir(root):
        continue

    for name in files:
        path = os.path.join(root, name)
        if is_text_file(name):
            normalize_line_endings(path)

print("Done. All line endings are now LF.")
