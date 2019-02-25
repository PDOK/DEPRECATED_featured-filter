# DEPRECATED

# featured-filter

Test case for filtering not needed updates


## Usage
For example do:

```
(featured-filter.test-set/create-json-files "testset" "col1" 1 5)

(do
  (-main ".test-files/inputset-0.json" "required-1,required-2")
  (-main ".test-files/inputset-1.json" "required-1,required-2")
  (-main ".test-files/inputset-2.json" "required-1,required-2")
  (-main ".test-files/inputset-3.json" "required-1,required-2")
  (-main ".test-files/inputset-4.json" "required-1,required-2")
  (-main ".test-files/inputset-5.json" "required-1,required-2"))

```

Input files are random, so mileage may vary. If you want to be sure you have not needed updated, create some static files. (The test-set generator comes in handy if you want big test sets.)

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
