"""Fill `poster` / `backdrop` in data/releases.json from TMDB.

Usage:  python3 tools/posters.py [--all] [--dry-run]

Looks up every title that has no poster yet (or all titles with --all), matches it by title, language and
year, and stores TMDB image URLs. Series seasons ("Show Season 3") get that season's poster when TMDB has one.
Titles with no confident match are left alone, and the app draws its own artwork for them.

The TMDB read token comes from $TMDB_TOKEN or ~/.config/streamlens/tmdb_token and is never written to the
repo. Uses api.tmdb.org (TMDB's official alias) because some Indian ISPs block api.themoviedb.org.
"""
import difflib
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
from datetime import date

API = "https://api.tmdb.org/3"
IMG = "https://image.tmdb.org/t/p/"
FEED = os.path.join(os.path.dirname(__file__), "..", "data", "releases.json")


def token():
    t = os.environ.get("TMDB_TOKEN")
    if not t:
        path = os.path.expanduser("~/.config/streamlens/tmdb_token")
        t = open(path).read().strip() if os.path.exists(path) else None
    if not t:
        sys.exit("No TMDB token: set TMDB_TOKEN or write it to ~/.config/streamlens/tmdb_token")
    return t


def get(path, **params):
    url = f"{API}{path}?{urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})}"
    for attempt in range(3):
        out = subprocess.run(
            ["curl", "-s", "-m", "20", "-H", f"Authorization: Bearer {TOKEN}", "-H", "Accept: application/json", url],
            capture_output=True, text=True,
        )
        if out.returncode == 0 and out.stdout.startswith("{"):
            return json.loads(out.stdout)
        time.sleep(1 + attempt)
    return {}


def norm(s):
    return re.sub(r"[^a-z0-9]+", "", (s or "").lower())


SEASON = re.compile(r"^(.*?)[\s:–-]*\(?\s*season\s*(\d+)\)?\s*$", re.I)


def score(cand, title, lang, year, is_series, is_season):
    names = [cand.get("title") or cand.get("name"), cand.get("original_title") or cand.get("original_name")]
    t = norm(title)
    best = 0.0
    for n in names:
        n = norm(n)
        if not n:
            continue
        if n == t:
            best = max(best, 3.0)
        elif t and (n.startswith(t) or t.startswith(n)) and min(len(n), len(t)) >= 4:
            best = max(best, 2.0)
        else:
            best = max(best, 3.0 * difflib.SequenceMatcher(None, n, t).ratio() - 1.0)
    s = best
    if cand.get("original_language") == lang:
        s += 1.5
    elif lang in ("te", "hi"):
        s -= 2.0  # an Indian title matched to a foreign-language film is almost always wrong
    when = cand.get("release_date") or cand.get("first_air_date") or ""
    if year and when[:4].isdigit() and not is_season:
        gap = abs(int(when[:4]) - year)
        s += {0: 2.0, 1: 1.0}.get(gap, -2.0)
    if not cand.get("poster_path"):
        s -= 5
    return s


def lookup(r):
    title, lang, year = r["title"], r.get("lang"), r.get("year")
    is_series = r.get("kind") == "series"
    m = SEASON.match(title)
    season = int(m.group(2)) if (is_series and m) else None
    query = m.group(1).strip() if season else title
    kind = "tv" if is_series else "movie"
    year_param = {"first_air_date_year": None if season else year} if is_series else {"year": year}
    results = get(f"/search/{kind}", query=query, include_adult="false", **year_param).get("results", [])
    if not results:
        results = get(f"/search/{kind}", query=query, include_adult="false").get("results", [])
    if not results:
        return None
    ranked = sorted(results[:10], key=lambda c: score(c, query, lang, year, is_series, bool(season)), reverse=True)
    best = ranked[0]
    if score(best, query, lang, year, is_series, bool(season)) < 4.0:
        return None
    poster = best.get("poster_path")
    if season:
        sp = get(f"/tv/{best['id']}/season/{season}").get("poster_path")
        poster = sp or poster
    return {
        "tmdbId": best["id"],
        "tmdbType": kind,
        "poster": IMG + "w500" + poster,
        "backdrop": (IMG + "w780" + best["backdrop_path"]) if best.get("backdrop_path") else None,
    }


def main():
    every = "--all" in sys.argv
    dry = "--dry-run" in sys.argv
    feed = json.load(open(FEED))
    found = missed = 0
    for r in feed["titles"]:
        if r.get("poster") and not every:
            continue
        hit = lookup(r)
        if hit:
            found += 1
            print(f"  + {r['title']} -> tmdb {hit['tmdbType']}/{hit['tmdbId']}")
            if not dry:
                r.update({k: v for k, v in hit.items() if v})
        else:
            missed += 1
            print(f"  - {r['title']}: no confident match")
        time.sleep(0.05)
    print(f"posters: {found} found, {missed} without a match")
    if not dry and found:
        json.dump(feed, open(FEED, "w"), ensure_ascii=False, indent=1)


TOKEN = token()
if __name__ == "__main__":
    main()
