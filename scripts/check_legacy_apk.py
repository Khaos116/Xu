"""After assembleNormalRelease: python scripts/check_legacy_apk.py"""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
output = root / "app/build/outputs/apk/normal/release"
metadata = json.loads((output / "output-metadata.json").read_text(encoding="utf-8"))
assert metadata["variantName"] == "normalRelease"
source = output / metadata["elements"][0]["outputFile"]
archives = list((root / "APK/Release").glob(f"{source.stem}_*.apk"))
assert archives, "Normal APK was not archived"
archive = max(archives, key=lambda path: path.stat().st_mtime_ns)
assert hashlib.sha256(source.read_bytes()).digest() == hashlib.sha256(archive.read_bytes()).digest(), "Archived APK differs from build output"
print(f"PASS: Normal APK archive matches build output: {archive}")
