k3b trying to modernize droidshows

v 250125 restructure file layout to make project compatible with gradle/androidstudio
    done: able to compile and debug in adroidstudio
v 250412 migrate to oldest androidX 1.0.0

Sub-Goals

* get rid of deprecated ListActivity
  * by replacing with AppCompatActivity + RecyclerView 
* make backup/restore work again under android5 and later 
* add app permission callback