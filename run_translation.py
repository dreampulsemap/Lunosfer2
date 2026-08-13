import os
import json
import urllib.request
import urllib.error
import time
from xml.etree import ElementTree as ET

API_KEY = os.environ.get('GEMINI_API_KEY')

def get_keys(file_path):
    keys = []
    if not os.path.exists(file_path): return set()
    tree = ET.parse(file_path)
    root = tree.getroot()
    for child in root:
        if child.tag == 'string':
            keys.append(child.attrib['name'])
    return set(keys)

def get_string_dict(file_path):
    strings = {}
    if not os.path.exists(file_path): return {}
    tree = ET.parse(file_path)
    root = tree.getroot()
    for child in root:
        if child.tag == 'string':
            strings[child.attrib['name']] = child.text
    return strings

en_strings = get_string_dict("app/src/main/res/values/strings.xml")
tr_keys = get_keys("app/src/main/res/values-tr/strings.xml")

tasks = {
    "ar": "Arabic",
    "hi": "Hindi",
    "zh": "Chinese (Simplified)",
    "de": "German",
    "es": "Spanish",
    "fr": "French",
    "ja": "Japanese",
    "pt": "Portuguese",
    "ru": "Russian"
}

def call_gemini(prompt):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={API_KEY}"
    data = json.dumps({
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseMimeType": "application/json"}
    }).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    
    for _ in range(5):
        try:
            with urllib.request.urlopen(req, timeout=60) as response:
                result = json.loads(response.read().decode('utf-8'))
                text = result['candidates'][0]['content']['parts'][0]['text']
                if text.startswith('```json'): text = text[7:]
                if text.startswith('```'): text = text[3:]
                if text.endswith('```'): text = text[:-3]
                return text.strip()
        except urllib.error.HTTPError as e:
            print(f"HTTPError: {e.code} {e.reason}")
            time.sleep(30)
        except Exception as e:
            print(f"Error: {e}")
            time.sleep(5)
    raise Exception("Failed after retries")

for code, lang in tasks.items():
    file_path = f"app/src/main/res/values-{code}/strings.xml"
    if not os.path.exists(file_path):
        continue
    
    current_keys = get_keys(file_path)
    missing_keys = tr_keys - current_keys
    
    if not missing_keys:
        print(f"[{code}] is up to date.")
        continue
        
    print(f"[{code}] Missing {len(missing_keys)} keys.")
    target_dict = {k: en_strings[k] for k in missing_keys if k in en_strings}
    
    items = list(target_dict.items())
    
    # Split into chunks of max 40 keys
    chunk_size = 40
    chunks = [dict(items[i:i+chunk_size]) for i in range(0, len(items), chunk_size)]
    
    translated = {}
    for i, chunk in enumerate(chunks):
        print(f"  Chunk {i+1}/{len(chunks)} for {code}...")
        prompt = f"""You are an Android application translator. Translate the JSON keys values to {lang}.
Return ONLY valid JSON. Escape all inner double quotes properly. Do NOT put comments.
Keep '%1$s', '%1$d', '%1$s%%' exactly as they are! Do NOT escape single quotes with XML, leave them as raw single quotes.
JSON to translate:
{json.dumps(chunk, indent=2)}"""

        translated_json = call_gemini(prompt)
        try:
            translated.update(json.loads(translated_json))
        except Exception as e:
            print(f"JSON Parse Error for {code}: {e}")
            print(translated_json)
            continue
        time.sleep(5)

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_xml = "\n"
    for k, v in translated.items():
        if v is None:
            continue
        escaped = str(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace('"', '\\"')
        new_xml += f'    <string name="{k}">{escaped}</string>\n'

    content = content.replace("</resources>", new_xml + "</resources>")
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Updated {file_path}")

