# Runs a make target inside the Nano Java build container (Windows).
#   .\build.ps1            build everything
#   .\build.ps1 test       run the tests
#   .\build.ps1 shell      open a shell in the container
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$image = 'nanojava-build'

docker image inspect $image *> $null
if ($LASTEXITCODE -ne 0) {
    docker build -t $image docker
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if ($args.Count -eq 1 -and $args[0] -eq 'shell') {
    docker run --rm -it -v "${PWD}:/src" -w /src $image bash
} else {
    docker run --rm -v "${PWD}:/src" -w /src $image make @args
}
exit $LASTEXITCODE
