ES_DIR=../../elasticsearch
grep "controller.registerHandler\(([^,]*),[^,]*" $ES_DIR -o -h -E -R --include "*.java" | \
  sed s/controller\.registerHandler\(// | sed s/RestRequest.Method.// | \
  sort -t, -k1 -k2 > methods.txt

  