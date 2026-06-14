import urllib.request
import urllib.parse
import json
import os
import ssl

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
BBOX = "46.228,6.035,46.238,6.058"
query = f"""[out:json][timeout:90];
(
  way["building"]({BBOX});
  relation["building"]({BBOX});
);
out geom;"""

print("Fetching CERN Meyrin building data from Overpass API (urllib)...")
data_encoded = urllib.parse.urlencode({'data': query}).encode('utf-8')

# Using a unique identifying User-Agent as required by the Overpass API usage policy
# to prevent being blocked (they block standard browser spoofing strings)
headers = {
    'User-Agent': 'CampusPulse3DMapProject/1.0 (contact: parthshroff@example.com)',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Accept': 'application/json'
}

req = urllib.request.Request(OVERPASS_URL, data=data_encoded, headers=headers, method='POST')

try:
    # Create an unverified SSL context to bypass macOS local issuer certificate errors
    context = ssl._create_unverified_context()
    with urllib.request.urlopen(req, context=context) as response:
        res_data = response.read().decode('utf-8')
        data = json.loads(res_data)
except Exception as e:
    print(f"Error fetching data: {e}")
    exit(1)

features = []
for element in data.get('elements', []):
    elem_type = element.get('type')
    tags = element.get('tags', {})
    
    if elem_type == 'way' and 'geometry' in element:
        coords = [[pt['lon'], pt['lat']] for pt in element['geometry']]
        if coords and coords[0] != coords[-1]:
            coords.append(coords[0])
        feature = {
            "type": "Feature",
            "id": f"way/{element['id']}",
            "properties": tags,
            "geometry": {
                "type": "Polygon",
                "coordinates": [coords]
            }
        }
        features.append(feature)
    elif elem_type == 'relation' and 'members' in element:
        outer_coords = []
        for member in element['members']:
            if member.get('role') == 'outer' and 'geometry' in member:
                coords = [[pt['lon'], pt['lat']] for pt in member['geometry']]
                if coords:
                    if coords[0] != coords[-1]:
                        coords.append(coords[0])
                    outer_coords.append(coords)
        if outer_coords:
            feature = {
                "type": "Feature",
                "id": f"relation/{element['id']}",
                "properties": tags,
                "geometry": {
                    "type": "MultiPolygon" if len(outer_coords) > 1 else "Polygon",
                    "coordinates": outer_coords if len(outer_coords) > 1 else [outer_coords[0]]
                }
            }
            features.append(feature)

geojson = {
    "type": "FeatureCollection",
    "features": features
}

output_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "../frontend/public"))
os.makedirs(output_dir, exist_ok=True)
output_path = os.path.join(output_dir, "cern_buildings.geojson")

with open(output_path, "w") as f:
    json.dump(geojson, f, indent=2)

print(f"Successfully saved {len(features)} buildings to {output_path}")
