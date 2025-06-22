k3b trying to modernize droidshows

v 250125 restructure file layout to make project compatible with gradle/androidstudio
    done: able to compile and debug in adroidstudio
v 250412 migrate to oldest androidX 1.0.0

v 250414 implemented backup/restore for android5ff with write-permissions+saf

Sub-Goals

* Method to create outfilename+extension (i.e. instead of name+ext+0): add yymmdd suffix
* ?? add thumbs to backup/restore (?? to zip ??) so to avoid reload data from internet 
> import/export as (?zipped?) csv, xmp. db. Export only md, html
> * * change logic from pick outfile to pick outdir to letting the define the filename  
> * * * predefined csv-filename
> * * id = seriesidXyearXseasonIdXepisodeId
* refactor backup/restore logic to util modul

* * get rid of deprecated ListActivity
  * by replacing with AppCompatActivity + RecyclerView 
