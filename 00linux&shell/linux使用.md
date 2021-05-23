## linux使用

```

[appuser@hadoop101 linuxshell]$ date +"%Y%m%d %H:%M:%S"
20210523 14:36:49

```

#### 2.1 Linux用户和用户组
UID
- 在添加普通用户时，系统默认用户ID从500开始编号
- 系统用户的ID范围是1~499
> 系统运行时必须有的用户，比如使用系统用户apache来运行httpd进程
- 根用户也就是root用户，它的ID是0

GID：也就是group id，用户组ID
```
[appuser@hadoop101 linuxshell]$ id
uid=1000(appuser) gid=1000(appuser) groups=1000(appuser)
[appuser@hadoop101 linuxshell]$ groups
appuser
[appuser@hadoop101 linuxshell]$ who
appuser  pts/0        2021-05-23 14:32 (hphost)
```
