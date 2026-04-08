param(
   [string]$ImageName = "momot-rest-test",
   [string]$ContainerName = "momot-rest-minimal-test",
   [int]$Port = 8081,
   [switch]$SkipBuild,
   [switch]$KeepContainer,
   [switch]$KeepArtifacts
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

$requiredPaths = @(
   "stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot",
   "stack-example-minimal/model/stack.ecore",
   "stack-example-minimal/model/stack.henshin",
   "stack-example-minimal/model/input/model/model_five_stacks.xmi"
)

foreach($path in $requiredPaths) {
   if(-not (Test-Path $path)) {
      throw "Required file is missing: $path"
   }
}

if(-not (Get-Command docker -ErrorAction SilentlyContinue)) {
   throw "docker command not found"
}
if(-not (Get-Command jar -ErrorAction SilentlyContinue)) {
   throw "jar command not found (JDK required)"
}
if(-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
   throw "curl.exe command not found"
}

function Invoke-CheckedCommand {
   param(
      [Parameter(Mandatory = $true)]
      [ScriptBlock]$Command,
      [Parameter(Mandatory = $true)]
      [string]$ErrorMessage
   )
   & $Command
   if($LASTEXITCODE -ne 0) {
      throw "$ErrorMessage (exit code $LASTEXITCODE)"
   }
}

if(-not $SkipBuild) {
   Write-Host "[1/8] Building Docker image $ImageName..."
   Invoke-CheckedCommand -Command { docker build -t $ImageName -f Dockerfile . | Out-Host } -ErrorMessage "Docker image build failed"
}

Write-Host "[2/8] Resetting test container $ContainerName..."
$existing = docker ps -aq --filter "name=$ContainerName"
if($existing) {
   docker rm -f $ContainerName | Out-Null
}

Write-Host "[3/8] Starting container on localhost:$Port -> 8080..."
Invoke-CheckedCommand -Command { docker run -d --name $ContainerName -p "${Port}:8080" $ImageName | Out-Host } -ErrorMessage "Container start failed"

$healthUrl = "http://localhost:$Port/health"
$ready = $false
for($i = 0; $i -lt 30; $i++) {
   try {
      $response = Invoke-WebRequest -UseBasicParsing $healthUrl
      if($response.StatusCode -eq 200) {
         $ready = $true
         break
      }
   } catch {
      # best effort retry
   }
   [System.Threading.Thread]::Sleep(1000)
}
if(-not $ready) {
   throw "Container did not become healthy at $healthUrl"
}

$jobRoot = "headless-example/job-minimal"
$jobZip = "headless-example/job-minimal.zip"
$responseZip = "headless-example/response-minimal.zip"
$responseDir = "headless-example/response-minimal"

if(Test-Path $jobRoot) { Remove-Item -Recurse -Force $jobRoot }
if(Test-Path $jobZip) { Remove-Item -Force $jobZip }
if(Test-Path $responseZip) { Remove-Item -Force $responseZip }
if(Test-Path $responseDir) { Remove-Item -Recurse -Force $responseDir }

Write-Host "[4/8] Creating deterministic minimal payload..."
New-Item -ItemType Directory -Force -Path "$jobRoot/src/at/ac/tuwien/big/momot/examples/stack" | Out-Null
New-Item -ItemType Directory -Force -Path "$jobRoot/model/input/model" | Out-Null
Copy-Item "stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" "$jobRoot/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
Copy-Item "stack-example-minimal/model/stack.ecore" "$jobRoot/model/stack.ecore"
Copy-Item "stack-example-minimal/model/stack.henshin" "$jobRoot/model/stack.henshin"
Copy-Item "stack-example-minimal/model/input/model/model_five_stacks.xmi" "$jobRoot/model/input/model/model_five_stacks.xmi"

Write-Host "[5/8] Building payload zip with stable entry names..."
Push-Location $jobRoot
Invoke-CheckedCommand -Command { jar --create --file ../job-minimal.zip model/stack.ecore model/stack.henshin model/input/model/model_five_stacks.xmi src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot | Out-Host } -ErrorMessage "Payload zip creation failed"
Pop-Location

Write-Host "[6/8] Executing /run request..."
$runUrl = "http://localhost:$Port/run?script=src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
Invoke-CheckedCommand -Command { curl.exe -sS -X POST $runUrl -H "Content-Type: application/zip" --data-binary "@$jobZip" --output $responseZip | Out-Host } -ErrorMessage "REST /run request failed"

Write-Host "[7/8] Extracting and validating response..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory((Resolve-Path $responseZip), (Join-Path (Get-Location) $responseDir))

$exitCodePath = Join-Path $responseDir "runner/exit_code.txt"
$runnerLogPath = Join-Path $responseDir "runner/runner.log"
$requestPath = Join-Path $responseDir "runner/request.json"

if(-not (Test-Path $exitCodePath)) {
   throw "Missing response artifact: $exitCodePath"
}
$exitCode = [int](Get-Content $exitCodePath)

Write-Host "Exit code: $exitCode"
if(Test-Path $requestPath) {
   Write-Host "Request metadata:"
   Get-Content -Raw $requestPath | Out-Host
}
if(Test-Path $runnerLogPath) {
   Write-Host "Runner log tail:"
   Get-Content $runnerLogPath -Tail 40 | Out-Host
}

if($exitCode -ne 0) {
   throw "Minimal REST test failed with exit_code=$exitCode"
}

Write-Host "[8/8] SUCCESS: Minimal REST test passed (exit_code=0)."
Write-Host "Swagger UI: http://localhost:$Port/docs"
Write-Host "OpenAPI:    http://localhost:$Port/openapi.json"

if(-not $KeepContainer) {
   docker rm -f $ContainerName | Out-Null
}
if(-not $KeepArtifacts) {
   if(Test-Path $jobRoot) { Remove-Item -Recurse -Force $jobRoot }
   if(Test-Path $jobZip) { Remove-Item -Force $jobZip }
   if(Test-Path $responseZip) { Remove-Item -Force $responseZip }
   if(Test-Path $responseDir) { Remove-Item -Recurse -Force $responseDir }
}
