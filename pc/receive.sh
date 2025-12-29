adb exec-out run-as com.example.mirror \
  sh -c 'exec /system/bin/app_process /system/bin com.example.mirror.ScreenService' \
| ffplay -fflags nobuffer -framedrop -
