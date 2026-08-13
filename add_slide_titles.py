import os
import xml.etree.ElementTree as ET

translations = {
    "en": {
        "dream_slide_title_0": "Dream Image",
        "dream_slide_title_1": "Dream Text",
        "dream_slide_title_2": "AI Analysis"
    },
    "tr": {
        "dream_slide_title_0": "Rüya Görseli",
        "dream_slide_title_1": "Rüya Metni",
        "dream_slide_title_2": "AI Analizi"
    },
    "de": {
        "dream_slide_title_0": "Traumbild",
        "dream_slide_title_1": "Traumtext",
        "dream_slide_title_2": "KI-Analyse"
    },
    "es": {
        "dream_slide_title_0": "Imagen del sueño",
        "dream_slide_title_1": "Texto del sueño",
        "dream_slide_title_2": "Análisis de IA"
    },
    "fr": {
        "dream_slide_title_0": "Image du rêve",
        "dream_slide_title_1": "Texte du rêve",
        "dream_slide_title_2": "Analyse de l'IA"
    },
    "ja": {
        "dream_slide_title_0": "夢の画像",
        "dream_slide_title_1": "夢のテキスト",
        "dream_slide_title_2": "AI分析"
    },
    "pt": {
        "dream_slide_title_0": "Imagem do Sonho",
        "dream_slide_title_1": "Texto do Sonho",
        "dream_slide_title_2": "Análise de IA"
    },
    "ru": {
        "dream_slide_title_0": "Изображение сна",
        "dream_slide_title_1": "Текст сна",
        "dream_slide_title_2": "ИИ-анализ"
    },
    "zh": {
        "dream_slide_title_0": "梦境图像",
        "dream_slide_title_1": "梦境文本",
        "dream_slide_title_2": "AI分析"
    },
    "ar": {
        "dream_slide_title_0": "صورة الحلم",
        "dream_slide_title_1": "نص الحلم",
        "dream_slide_title_2": "تحليل الذكاء الاصطناعي"
    },
    "hi": {
        "dream_slide_title_0": "सपना छवि",
        "dream_slide_title_1": "सपना पाठ",
        "dream_slide_title_2": "एआई विश्लेषण"
    }
}

res_dir = "app/src/main/res"
for folder in os.listdir(res_dir):
    if folder.startswith("values"):
        lang = folder.split("-")[1] if "-" in folder else "en"
        xml_file = os.path.join(res_dir, folder, "strings.xml")
        if os.path.exists(xml_file):
            tree = ET.parse(xml_file)
            root = tree.getroot()
            
            # Remove existing keys
            for child in list(root):
                if child.attrib.get('name') in ['dream_slide_title_0', 'dream_slide_title_1', 'dream_slide_title_2']:
                    root.remove(child)
            
            # Use specific language or fallback to English
            lang_dict = translations.get(lang, translations["en"])
            
            for key in ["dream_slide_title_0", "dream_slide_title_1", "dream_slide_title_2"]:
                el = ET.SubElement(root, "string", name=key)
                el.text = lang_dict[key]
            
            tree.write(xml_file, encoding="utf-8", xml_declaration=True)

print("Added slide titles.")
