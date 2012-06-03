AndroidTimeLock
===============

Will time lock a build and stop the user running it if its too old, useful for releasing dev builds.

## Usage ##
Include the source or include as a library project.

Call in your application onCreate method:

    //Application.onCreate()
    TimeLock.get(this).setEnabled(..).set...;

Set any params you need, these are stored between instances. 
Sorted! One Week after the app expires it will fail to open.

Then in your BaseActivity call:

    // Activity.onCreate(Bundle bdl) { ...
    TimeLock.get(this).check();
    
This remembers any settings setup in your application onCreate call.

## Version ##
- 1.0 initial version - very basic only supports dialog
