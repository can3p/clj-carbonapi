PACKAGES=cohttp.async yojson atdgen
CBFLAGS=$(addprefix -pkg ,$(PACKAGES))

build: *.ml render_atd
	corebuild bonapicar.native ${CBFLAGS}

render_atd: render_json.atd
	atdgen -j -j-std render_json.atd
	atdgen -t render_json.atd

run: build
	./bonapicar.native
