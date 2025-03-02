# PowerShell script to install missing Python modules

Write-Host "Installing missing Python modules..."

# Function to check error level
function Check-ErrorLevel ($ErrorMessage) {
    if ($LASTEXITCODE -ne 0) {
        Write-Error $ErrorMessage
        pause
        exit 1
    }
}

# Check if Python is installed
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Check-ErrorLevel "Python is not installed. Please install Python and try again."
}

# Check if pip is installed
if (-not (Get-Command pip -ErrorAction SilentlyContinue)) {
    Write-Host "pip is not installed. Attempting to install pip..."
    python -m ensurepip --default-pip
    Check-ErrorLevel "Failed to install pip. Please install pip manually."
    Write-Host "pip installed successfully."
}

# Install the required modules
Write-Host "Installing torch..."
python -m pip install torch
Check-ErrorLevel "Failed to install torch."

Write-Host "Installing ultralytics..."
python -m pip install ultralytics
Check-ErrorLevel "Failed to install ultralytics."

Write-Host "Installing onnxsim..."
python -m pip install onnxsim
Check-ErrorLevel "Failed to install onnxsim."

Write-Host "All modules installed successfully!"
pause
exit 0