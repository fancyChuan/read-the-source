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
第2种方式的衍生用法是将xxx.sh设置为系统命令，那么只需要将xxx.sh 放到 $PATH 变量所包含的目录下即可。

> 为了更清晰地看到脚本运行的过程，还可以借助-x参数来观察脚本的运行情况

点号用于执行某个脚本，甚至脚本没有可执行权限也可以运行
```
. ./xxx.sh 
```
与点号类似，source命令也可读取并在当前环境中执行脚本，同时还可返回脚本中最后一个命令的返回状态；如果没有返回值则返回0，代表执行成功

声明变量：declare、typeset

从一个循环（for、while、until或者select）中退出。break后可以跟一个数字n，代表跳出n层循环，n必须大于1


eval
- 用法1：将字符串解析成命令执行
- 用法2：程序运行中根据某个变量确定实际的变量名
```
[appuser@hadoop101 linuxshell]$ name1=66
[appuser@hadoop101 linuxshell]$ no=1
[appuser@hadoop101 linuxshell]$ eval echo "$"name$no
66
```
- 用法3：将一个变量的值作为另一个变量名，并赋值
```
[appuser@hadoop101 linuxshell]$ name=result
[appuser@hadoop101 linuxshell]$ value=bigdata
[appuser@hadoop101 linuxshell]$ eval $name=$value
[appuser@hadoop101 linuxshell]$ echo $result
bigdata
```