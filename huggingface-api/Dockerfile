# Use the official Python image from the Docker Hub
FROM python:3.10
# Set the working directory in the container
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY . /app

# Install any needed packages
RUN pip install --prefer-binary --no-cache-dir huggingface-hub confluent-kafka pymongo

RUN pip install --prefer-binary llama-cpp-python \
    --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cpu --verbose

# Run the application
ENTRYPOINT python app.py tail -f /dev/null
