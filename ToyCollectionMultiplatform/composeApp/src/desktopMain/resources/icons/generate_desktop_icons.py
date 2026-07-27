import os
import sys
import subprocess
import struct

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    src_png = os.path.normpath(os.path.join(base_dir, "../../../androidMain/res/drawable/icon.png"))
    
    if not os.path.exists(src_png):
        print(f"Error: Android icon not found at {src_png}")
        sys.exit(1)
        
    print(f"Using source icon: {src_png}")
    
    # 0. Generate temporary rounded PNG
    temp_rounded_png = os.path.join(base_dir, "temp_rounded.png")
    print("Rounding icon corners...")
    subprocess.run(["/Users/luizvaldetaro/.gemini/antigravity/scratch/round_image", src_png, temp_rounded_png, "0.18"], check=True)
    
    # 1. Create temporary iconset directory
    iconset_dir = os.path.join(base_dir, "icon.iconset")
    os.makedirs(iconset_dir, exist_ok=True)
    
    # Sizes required for Mac icns
    sizes = [
        ("16x16", 16),
        ("16x16@2x", 32),
        ("32x32", 32),
        ("32x32@2x", 64),
        ("128x128", 128),
        ("128x128@2x", 256),
        ("256x256", 256),
        ("256x256@2x", 512),
        ("512x512", 512),
        ("512x512@2x", 1024)
    ]
    
    for name, size in sizes:
        dest_png = os.path.join(iconset_dir, f"icon_{name}.png")
        # Run sips to resize
        subprocess.run(["sips", "-z", str(size), str(size), temp_rounded_png, "--out", dest_png], check=True, stdout=subprocess.DEVNULL)
        
    # 2. Run iconutil to create .icns
    icns_path = os.path.join(base_dir, "icon.icns")
    subprocess.run(["iconutil", "-c", "icns", iconset_dir, "-o", icns_path], check=True)
    print(f"Generated ICNS icon at: {icns_path}")
    
    # 3. Clean up iconset directory and its files
    for name, _ in sizes:
        os.remove(os.path.join(iconset_dir, f"icon_{name}.png"))
    os.rmdir(iconset_dir)
    
    # 4. Generate .ico for Windows (using 256x256 size)
    temp_ico_png = os.path.join(base_dir, "temp_ico_256.png")
    subprocess.run(["sips", "-z", "256", "256", temp_rounded_png, "--out", temp_ico_png], check=True, stdout=subprocess.DEVNULL)
    
    with open(temp_ico_png, "rb") as f:
        png_data = f.read()
        
    os.remove(temp_ico_png)
    
    ico_path = os.path.join(base_dir, "icon.ico")
    png_size = len(png_data)
    
    header = struct.pack("<HHH", 0, 1, 1)
    entry = struct.pack("<BBBBHHII", 0, 0, 0, 0, 1, 32, png_size, 22)
    
    with open(ico_path, "wb") as f:
        f.write(header)
        f.write(entry)
        f.write(png_data)
        
    print(f"Generated ICO icon at: {ico_path}")

    # Remove temporary rounded PNG
    os.remove(temp_rounded_png)

if __name__ == "__main__":
    main()
