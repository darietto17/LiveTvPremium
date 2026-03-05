#!/usr/bin/env python3
import json
import os
import gzip

def generate_json_api():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.dirname(script_dir)
    
    film_cache_path = os.path.join(script_dir, "film_cache.json")
    serie_cache_path = os.path.join(script_dir, "serie_cache.json")
    categories_path = os.path.join(script_dir, "categories.json")
    
    output_json = os.path.join(root_dir, "app_data.json")
    output_gz = os.path.join(root_dir, "app_data.json.gz")
    
    data = {
        "last_update": "",
        "categories": [],
        "movies": [],
        "series": []
    }
    
    # Load Categories
    if os.path.exists(categories_path):
        with open(categories_path, 'r', encoding='utf-8') as f:
            data["categories"] = json.load(f)
            
    # Load Movies from cache
    if os.path.exists(film_cache_path):
        with open(film_cache_path, 'r', encoding='utf-8') as f:
            movies_dict = json.load(f)
            # Map into M3UItem-like structure for the app
            for mid, m in movies_dict.items():
                data["movies"].append({
                    "name": m.get("title", ""),
                    "groupTitle": "Film", # simplified or mapping logic
                    "tvgId": str(m.get("id", "")),
                    "tvgLogo": f"https://image.tmdb.org/t/p/w500{m.get('poster_path', '')}" if m.get('poster_path') else "",
                    "url": f"https://vixsrc.to/movie/{m['id']}/?lang=it"
                })

    # Load Series from cache
    if os.path.exists(serie_cache_path):
        with open(serie_cache_path, 'r', encoding='utf-8') as f:
            series_dict = json.load(f)
            # Future: add series mapping
            
    # Write JSON
    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False)
        
    # Gzip JSON
    import shutil
    with open(output_json, 'rb') as f_in:
        with gzip.open(output_gz, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)
            
    print(f"Generated {output_json} and {output_gz}")

if __name__ == "__main__":
    generate_json_api()
