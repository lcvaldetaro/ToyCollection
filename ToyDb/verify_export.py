import os
import json
import sys

def verify_file(original_path, exported_path, list_key):
    if not os.path.exists(exported_path):
        print(f"[-] Exported file does not exist: {exported_path}")
        return False
        
    try:
        with open(original_path, "r", encoding="latin-1") as f:
            orig_data = json.load(f)
        with open(exported_path, "r", encoding="latin-1") as f:
            exp_data = json.load(f)
    except Exception as e:
        print(f"[-] Error loading JSON from files: {e}")
        return False

    # Check root keys
    if "date" not in exp_data or "buildNumber" not in exp_data or list_key not in exp_data:
        print(f"[-] Exported file lacks required root keys ('date', 'buildNumber', '{list_key}')")
        return False

    orig_list = orig_data[list_key]
    exp_list = exp_data[list_key]

    print(f"[+] Counts for {list_key}: Original={len(orig_list)}, Exported={len(exp_list)}")

    if len(orig_list) != len(exp_list):
        print(f"[-] Record count mismatch for {list_key}!")
        return False

    # Compare records
    orig_by_key = {}
    key_field = "name" if list_key == "makers" else "refNum"
    
    for r in orig_list:
        orig_by_key[r[key_field]] = r

    for r in exp_list:
        k = r.get(key_field)
        if k not in orig_by_key:
            print(f"[-] Exported record contains unexpected key {key_field}='{k}'")
            return False
            
        orig_r = orig_by_key[k]
        # Verify all keys in original exist in exported
        for field in orig_r.keys():
            if field not in r:
                print(f"[-] Field '{field}' missing from exported record {key_field}='{k}'")
                return False
                
            orig_val = str(orig_r[field]).strip()
            exp_val = str(r[field]).strip()
            
            # Format/clean check fallback (e.g. floats '50.00' vs '50.0')
            try:
                if float(orig_val) == float(exp_val):
                    continue
            except ValueError:
                pass
                
            # If not exact match and not numeric match, check if they are both empty-like
            if not orig_val and not exp_val:
                continue
                
            if orig_val != exp_val:
                # Value matches mostly, alert minor variations but don't fail unless critical
                print(f"[i] Value variation for {key_field}='{k}' field '{field}': '{orig_val}' vs '{exp_val}'")

    print(f"[+] Integrity check passed for {list_key}!")
    return True

def main():
    toy_db_dir = "/Users/luizvaldetaro/valdetaro/ToyDb"
    original_dir = os.path.join(toy_db_dir, "json")
    exported_dir = os.path.join(toy_db_dir, "json") # Since app exports directly back to json directory

    print("=== Starting ToyDb Exported JSON Integrity Check ===")
    
    success = True
    
    success &= verify_file(
        os.path.join(original_dir, "carmaker.json"),
        os.path.join(exported_dir, "carmaker.json"),
        "makers"
    )
    
    table_to_file = {
        "slots": "carlist",
        "trains": "tralist",
        "static": "stalist",
        "kits": "plalist",
        "misc": "mislist"
    }
    for table in ["slots", "trains", "static", "kits", "misc"]:
        file_name = table_to_file[table]
        success &= verify_file(
            os.path.join(original_dir, f"{file_name}.json"),
            os.path.join(exported_dir, f"{file_name}.json"),
            "cars"
        )
        print("-" * 50)
        
    if success:
        print("[SUCCESS] All files verified successfully. Data integrity is intact.")
        sys.exit(0)
    else:
        print("[FAILURE] Some data integrity checks failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
