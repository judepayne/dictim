	# 0.9.0.3
	- compatibility with d2 v0.6.9
	- dictim.flat is now a first class structure
	- new dictim.walk namespace with keywordize, stringify keys functions
	- added dictim.flat fns and dictim.walk fns to command line tool
	
	# 0.9.0.1
	- patch release to allow all variants of linear-gradient

	# 0.9.0
	- allow 'transparent' in style.fill
	- allow linear-gradient in style.fill. must be quoted, e.g. 'linear-gradient(#112233, #aabbcc)'

	# 0.8.9
	- Fixed #16. Allow banned characters in label when escaped or in quotes

	# 0.8.8
	- Fixed #14. See `lists` in wiki
	- Changed how comments are represented. See wiki
	- add wiki section on command line Windows string handling
	- Changed cmd line apply-template to not remove styles by default. Use the `-r` option to remove styles ahead of template application
	- other small fixes

	# 0.8.7
	- Fixed bug in cmd line apply-template (not watch) usage. The -t option was not being picked up

	# 0.8.6
	- changes to graphspec:
	  - added container->data and container-template
	  - add template
	

	# 0.8.5
        - added --graph/-g option to cmd line tool
	- added graphspec and tests
	  - graphspec allows you to express a diagram as a graph
	  - interface inspired by Zac Tellman's Rhizome
	  - interface is pure data, so can be used over the wire
	- Compile output improvements
	  - inline (put on single line) attrs where possible
	  - inlining controlled by an opts public atom
	  - fixed bug in ctr layout which inserted an additional space
	  - changed the formatter to work with new compile output

	# 0.8.4
	- added horizontal-gap, vertical-gap and grid-gap attrs

	# 0.8.3
	- apply-template added to cmd line tool
	- watching parse and watching compile added to cmd line tool
	- -r cmd line option with parse to remove styles

	# 0.8.2
	- fix to json parse

	# 0.8.1
	- Better prettified json from parse in cmd line tool

	# 0.8.0
	- Proper validity checking of d2 attributes
	
	# v.0.7.13
	- Minor release to support dictim.server switch to use dictim.tests
	# v.0.7.12
	- exposed additional fns in template ns: template-fn, set-attrs!, set-label!
	# v.0.7.11
	- bug fixes for 0.7.10 release. No new functionality
	# v.0.7.10
	- dictim Templates allow you to separate data from styling
	- added examples in examples/ folder to play with in cmd line tool --watch
	# v.0.7.00
	- Command Line Tool
	- Improved documentation
	- Improved CI
	- Switch to using main branch only for releases

	# v.0.6.1
	- Support for converting dictim to/from json

	# v0.6.0
	- A major catch up to d2 verion 0.6.3. Added support for compilation and parsing of:
	  - Grid diagram
	  - classes
	  - vars
	  - positions
	  - globs
	  - imports
	  - overrides
	  - array notation in attr and var values
	  - the 'null' keyword
	  - layers, scenarios & steps
	  - 'connection-references'/ 'conn-refs': referring to a connection via (a -> b)[0] notation
	- many other bug fixes
	- lots more tests
	- updated wiki documentation

	# v0.5.7
	- Numbers now allowed as shape keys
	- Compatible with d2 v0.2.4
