import os
import sqlite3
import json
import sys

def import_makers(conn, json_path):
    with open(json_path, 'r', encoding='latin-1') as f:
        data = json.load(f)
    cursor = conn.cursor()
    for m in data['makers']:
        cursor.execute(
            """
            INSERT OR REPLACE INTO makers (name, country, bitmaps, bitmaps_size, bitmaps_timestamp, comments)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (m.get('name'), m.get('country', ''), m.get('bitmaps', ''), m.get('bitmapsSize', ''), m.get('bitmapsTimeStamp', ''), m.get('comments', ''))
        )
    conn.commit()
    print(f"[+] Imported {len(data['makers'])} makers from {json_path}")

def import_toys(conn, category, json_path):
    with open(json_path, 'r', encoding='latin-1') as f:
        data = json.load(f)
    cursor = conn.cursor()
    for c in data['cars']:
        ref = int(c.get('refNum', '0'))
        val = float(c.get('value', '0').strip() or '0')
        paid = float(c.get('amountPaid', '0').strip() or '0')
        pic_size = int(c.get('pictureSize', '0').strip() or '0')
        pic_time = int(c.get('pictureTimeStamp', '0').strip() or '0')
        
        cursor.execute(
            """
            INSERT OR REPLACE INTO toys (
                ref_num, toy_type, description, maker_combo, scale, factory_car, 
                body_maker, acquired, chassis_type, chassis_maker, condition, color, 
                motor_maker, motor_details, catalog_number, comments, major_work, 
                minor_work, repro, value, amount_paid, amount_sold, traded, buy, 
                maintenance, to_make, detail, boxed, picture, picture_size, 
                picture_timestamp, has_picture, bitmaps, bitmaps_size, bitmaps_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                ref, category, c.get('description'), c.get('makerCombo', ''), c.get('scale', ''), c.get('factoryCar', ''),
                c.get('bodyMaker', ''), c.get('acquired', ''), c.get('chassisType', ''), c.get('chassisMaker', ''), c.get('condition', ''), c.get('color', ''),
                c.get('motorMaker', ''), c.get('motorDetails', ''), c.get('catalogNumber', ''), c.get('comments', ''), c.get('majorWork', ''),
                c.get('minorWork', ''), c.get('repro', ''), val, paid, c.get('amountSold', ''), c.get('traded', ''), c.get('buy', ''),
                c.get('maintenance', ''), c.get('toMake', ''), c.get('detail', ''), c.get('boxed', ''), c.get('picture', ''), pic_size,
                pic_time, c.get('hasPicture', ''), c.get('bitmaps', ''), c.get('bitmapsSize', ''), c.get('bitmapsTimeStamp', '')
            )
        )
    conn.commit()
    print(f"[+] Imported {len(data['cars'])} {category} toys from {json_path}")

def main():
    db_path = "composeApp/toydb.db"
    if not os.path.exists(db_path):
        print(f"[-] Database file not found at {db_path}")
        sys.exit(1)
        
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 1. Verify schema tables exist
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
    tables = [t[0] for t in cursor.fetchall()]
    print(f"[+] Tables in database: {tables}")
    for t in ['category_settings', 'makers', 'toys']:
        if t not in tables:
            print(f"[-] Missing table: {t}")
            sys.exit(1)
            
    # 2. Clear tables to perform clean import test
    cursor.execute("DELETE FROM toys")
    cursor.execute("DELETE FROM makers")
    conn.commit()
    
    # 3. Perform Imports
    import_makers(conn, "json/carmaker.json")
    import_toys(conn, "slot", "json/carlist.json")
    import_toys(conn, "train", "json/tralist.json")
    import_toys(conn, "static", "json/stalist.json")
    import_toys(conn, "kit", "json/plalist.json")
    import_toys(conn, "misc", "json/mislist.json")
    
    # 4. Perform CRUD checks
    print("\n--- Verifying Database CRUD ---\n")
    
    # Read count
    cursor.execute("SELECT COUNT(*) FROM toys")
    count_before = cursor.fetchone()[0]
    print(f"[+] Total toys in DB: {count_before}")
    
    # Create new toy
    cursor.execute(
        """
        INSERT INTO toys (ref_num, toy_type, description, value) 
        VALUES (9999, 'slot', 'Test Slot Car Ref 9999', 99.99)
        """
    )
    conn.commit()
    print("[+] CRUD: Created test toy ref 9999")
    
    # Read test
    cursor.execute("SELECT description, value FROM toys WHERE ref_num=9999 AND toy_type='slot'")
    toy = cursor.fetchone()
    print(f"[+] CRUD: Read toy ref 9999 -> Description: '{toy[0]}', Value: {toy[1]}")
    assert toy[0] == 'Test Slot Car Ref 9999' and toy[1] == 99.99
    
    # Update test
    cursor.execute(
        """
        UPDATE toys SET description='Updated Test Slot Car Ref 9999', value=120.50
        WHERE ref_num=9999 AND toy_type='slot'
        """
    )
    conn.commit()
    cursor.execute("SELECT description, value FROM toys WHERE ref_num=9999 AND toy_type='slot'")
    toy = cursor.fetchone()
    print(f"[+] CRUD: Updated toy ref 9999 -> Description: '{toy[0]}', Value: {toy[1]}")
    assert toy[0] == 'Updated Test Slot Car Ref 9999' and toy[1] == 120.50
    
    # Delete test
    cursor.execute("DELETE FROM toys WHERE ref_num=9999 AND toy_type='slot'")
    conn.commit()
    cursor.execute("SELECT COUNT(*) FROM toys WHERE ref_num=9999 AND toy_type='slot'")
    count_after_del = cursor.fetchone()[0]
    print(f"[+] CRUD: Deleted test toy. Count matching ref 9999 = {count_after_del}")
    assert count_after_del == 0
    
    conn.close()
    print("\n[SUCCESS] SQLite Schema and CRUD operations verified successfully.")
    
if __name__ == "__main__":
    main()
