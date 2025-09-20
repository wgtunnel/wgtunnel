#!/usr/bin/env python3

import xml.etree.ElementTree as ET
from pathlib import Path

# --- Configurable paths ---
SCRIPT_DIR = Path(__file__).resolve().parent
RES_DIR = SCRIPT_DIR.parent / "app" / "src" / "main" / "res"
SOURCE_STRINGS = RES_DIR / "values" / "strings.xml"

def get_string_keys(file_path):
    if not file_path.exists():
        print(f"⚠️  Source strings file not found: {file_path}")
        return set()
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        return {elem.attrib['name'] for elem in root if elem.tag == 'string' and 'name' in elem.attrib}
    except ET.ParseError as e:
        print(f"❌ Failed to parse {file_path}: {e}")
        return set()

def clean_translation_file(file_path, source_keys):
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()
        changed = False

        for elem in list(root):  # list() to allow removal during iteration
            if elem.tag == 'string' and 'name' in elem.attrib:
                if elem.attrib['name'] not in source_keys:
                    print(f"  ❌ Removing unused key: {elem.attrib['name']}")
                    root.remove(elem)
                    changed = True

        if changed:
            tree.write(file_path, encoding='utf-8', xml_declaration=True)
            print(f"✅ Cleaned: {file_path}")
        else:
            print(f"✅ No unused keys in: {file_path}")

    except ET.ParseError as e:
        print(f"❌ Failed to parse {file_path}: {e}")

def main():
    print(f"📂 Looking for translations in: {RES_DIR}")
    source_keys = get_string_keys(SOURCE_STRINGS)

    if not source_keys:
        print("⚠️  No source keys found. Aborting.")
        return

    for dir_path in RES_DIR.glob("values-*"):
        if dir_path.name == "values":
            continue  # skip base locale
        trans_file = dir_path / "strings.xml"
        if trans_file.exists():
            print(f"\n🔍 Checking: {trans_file}")
            clean_translation_file(trans_file, source_keys)

if __name__ == "__main__":
    main()
