#!/usr/bin/env bash
set -euo pipefail
if ! command -v ffmpeg >/dev/null || ! command -v ffprobe >/dev/null; then
  echo "Installing FFmpeg on this GitHub Actions runner for reproducible media QA"
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq ffmpeg
fi
command -v ffmpeg
command -v ffprobe
# Public-domain procedural testsrc2 + sine tones. No copyrighted videos or
# user gallery footage are used in automated media QA.
ffmpeg -hide_banner -loglevel error -y -f lavfi \
  -i 'testsrc2=size=1920x1080:rate=24' \
  -f lavfi -i 'sine=frequency=440:sample_rate=48000' \
  -t 2 -c:v libx264 -preset ultrafast -crf 24 -pix_fmt yuv420p \
  -threads 2 -c:a aac -b:a 96k -shortest qa-video.mp4
ffmpeg -hide_banner -loglevel error -y -i qa-video.mp4 \
  -map 0:v:0 -c:v copy -an qa-silent.mp4
ffprobe -v error -select_streams v:0 \
  -show_entries stream=codec_name,width,height -of default=noprint_wrappers=1 qa-video.mp4 |
  tee qa-video-input-probe.txt
ffprobe -v error -select_streams a:0 -show_entries stream=codec_name \
  -of default=noprint_wrappers=1 qa-video.mp4 | tee -a qa-video-input-probe.txt
grep -q 'codec_name=h264' qa-video-input-probe.txt
grep -q 'codec_name=aac' qa-video-input-probe.txt
grep -q 'width=1920' qa-video-input-probe.txt
grep -q 'height=1080' qa-video-input-probe.txt
test -s qa-silent.mp4
echo "Synthetic H.264/AAC and video-only MP4 fixtures ready."
