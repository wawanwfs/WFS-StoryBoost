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
# Extended real encoded H.264/AAC is slow enough to exercise non-zero
# asynchronous Media3 progress before cancellation on the API35 emulator.
ffmpeg -hide_banner -loglevel error -nostdin -y \
 -f lavfi -i testsrc2=size=320x568:rate=24 -f lavfi -i sine=frequency=620:sample_rate=48000 \
 -t 12 -c:v libx264 -pix_fmt yuv420p -preset ultrafast -crf 29 -c:a aac -b:a 80k \
 -movflags +faststart "$OUT/qa-h264-extended.mp4"
# Monochrome video isolates colored caption pixels without relying on a blank frame.
ffmpeg -hide_banner -loglevel error -nostdin -y \
 -f lavfi -i 'testsrc2=size=320x568:rate=24,format=gray,format=yuv420p' \
 -f lavfi -i sine=frequency=440:sample_rate=48000 -t 1.2 \
 -c:v libx264 -pix_fmt yuv420p -preset ultrafast -c:a aac -b:a 96k \
 -movflags +faststart "$OUT/qa-caption.mp4"
ffmpeg -hide_banner -loglevel error -nostdin -y -display_rotation 90 \
 -i "$OUT/qa-caption.mp4" -c copy "$OUT/qa-caption-rotated.mp4"
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
for name,angle in (('qa-caption.mp4',0),('qa-caption-rotated.mp4',90)):
    data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(root/name)]))
    assert {(s['codec_type'],s['codec_name']) for s in data['streams']}=={('video','h264'),('audio','aac')}
    rotation=[d['rotation'] for s in data['streams'] for d in s.get('side_data_list',[]) if 'rotation' in d]
    assert (abs(rotation[0]) if rotation else 0)==angle
    assert 1.1<=float(data['format']['duration'])<=1.3
    print(name,'caption-fixture',angle)
long_path=root/'qa-h264-extended.mp4'
long_data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(long_path)]))
assert {('video','h264'),('audio','aac')} <= {(s['codec_type'],s['codec_name']) for s in long_data['streams']}
assert 11.8<=float(long_data['format']['duration'])<=12.2
print(long_path.name,'extended-media3-cancel-fixture',long_path.stat().st_size)
PY

mkdir -p qa-provider/src/main/assets
cp "$OUT/qa-h264-extended.mp4" qa-provider/src/main/assets/
