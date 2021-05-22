# shell-linux
linux与shell相关知识



```
#!/bin/bash
```
"#!"告诉系统执行整个脚本的时候要使用 /bin/bash 这个解释器。但是，如果使用
```
bash xxx.sh 
```
这种方式来运行shell脚本时，那么"#!/bin/bash"就可以省略不写。

运行shell的两种方式：
- 方式1： bash xxx.sh 这种方式xxx.sh是普通文件即可
- 方式2： ./xxx.sh  这种方式要求xxx.sh 需要有可执行权限，否则执行的时候会报错
```
chmod +x ./xxx.sh
```