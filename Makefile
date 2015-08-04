PACKAGES=cohttp.async yojson
CBFLAGS=$(addprefix -pkg ,$(PACKAGES))

build: *.ml
	corebuild bonapicar.native ${CBFLAGS}

run: build
	./bonapicar.native
