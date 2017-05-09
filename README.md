Stew5
=====

Stew5 is a tiny utility tool for database via JDBC.

Stew5 requires Java 1.7 or later, and JDBC drivers to connect your DBMS.

See [the project's wiki page](https://github.com/argius/Stew5/wiki) for further information.


To Install
----------

Run the following command.

```sh
$ curl -fsSL http://bit.ly/inststew5 | sh
```

or

```sh
$ curl -fsSL https://goo.gl/2C55M4 | sh
```

Both of these urls are shortened of `https://raw.githubusercontent.com/argius/Stew5/master/install.sh`.

To uninstall, remove `~/.stew` and `$(which stew5)`.


You need only to download, see [the releases page](https://github.com/argius/Stew5/releases).


How To Start App
----------------

Run following commands.

```
# Run with GUI (Swing) mode
$ stew5 --gui

# Run with console mode
$ stew5 --cui

# Run commands and exit
$ stew5 --cui -c connector1 "select * from table1 limit 3"
```


License
-------

Apache License 2.0
