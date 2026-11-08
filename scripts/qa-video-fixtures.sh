#!/usr/bin/env bash
set -euo pipefail
OUT="app/src/androidTest/assets"
mkdir -p "$OUT"
for tool in ffmpeg ffprobe python3; do command -v "$tool" >/dev/null; done
ffmpeg -hide_banner -loglevel error -nostdin -y \
 -f lavfi -i testsrc2=size=320x568:rate=24 -f lavfi -i sine=frequency=880:sample_rate=48000 \
 -t 2 -c:v libx264 -pix_fmt yuv420p -preset ultrafast -crf 26 -c:a aac -b:a 96k \
 -movflags +faststart "$OUT/qa-h264-aac.mp4"
ffmpeg -hide_banner -loglevel error -nostdin -y -i "$OUT/qa-h264-aac.mp4" \
 -map 0:v:0 -c:v copy -an "$OUT/qa-h264-silent.mp4"
ffmpeg -hide_banner -loglevel error -nostdin -y -display_rotation 90 \
 -i "$OUT/qa-h264-aac.mp4" -c copy "$OUT/qa-rotated.mp4"
printf 'invalid media\n' > "$OUT/qa-invalid.mp4"
python3 - "$OUT" <<'PY'
import json, pathlib, subprocess, sys
root=pathlib.Path(sys.argv[1])
for name in ('qa-h264-aac.mp4','qa-h264-silent.mp4','qa-rotated.mp4'):
    path=root/name
    data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(path)]))
    codecs={(s['codec_type'],s['codec_name']) for s in data['streams']}
    assert ('video','h264') in codecs,(name,codecs)
    assert (('audio','aac') in codecs)==(name!='qa-h264-silent.mp4'),(name,codecs)
    assert 1.8<=float(data['format']['duration'])<=2.2
    if name=='qa-rotated.mp4':
        rotation=[x['rotation'] for s in data['streams'] for x in s.get('side_data_list',[]) if 'rotation' in x]
        assert rotation and abs(rotation[0])==90,rotation
    print(name,sorted(codecs),data['format']['duration'],path.stat().st_size)
PY
