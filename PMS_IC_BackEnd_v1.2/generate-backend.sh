#!/bin/bash

# PMS Backend Code Generator
# This script creates all necessary backend files

BASE_DIR="src/main/java/com/insuretech/pms"

echo "🚀 Generating PMS Backend Structure..."

# Create directory structure
echo "📁 Creating directory structure..."
mkdir -p "$BASE_DIR"/{auth,project,task,chat,risk,report,common}/{entity,repository,service,controller,dto,config}
mkdir -p src/main/resources
mkdir -p src/test/java/com/insuretech/pms

echo "✅ Directory structure created!"
echo ""
echo "📋 Next steps:"
echo "1. Run the Python script to generate all source files"
echo "2. Run: python generate-backend-files.py"
echo ""
