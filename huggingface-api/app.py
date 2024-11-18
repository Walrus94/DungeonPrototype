# from transformers import pipeline
import logging
import os
import json
from confluent_kafka import Consumer
from pymongo import MongoClient
from bson.objectid import ObjectId
from llama_cpp import Llama

logging.basicConfig(level=logging.DEBUG)

# Load the Hugging Face API token from the environment variable
hf_model_file = os.getenv('HUGGINGFACE_MODEL_FILE')
kafka_bootstrap_server = os.getenv('KAFKA_BOOTSTRAP_SERVERS')
kafka_topic_name = os.getenv("KAFKA_TOPIC_NAME")
mongo_database_port = os.getenv('MONGO_DB_PORT')
mongo_database_name = os.getenv('MONGO_DB_DATABASE')
mongo_database_user = os.getenv('MONGO_DB_USERNAME')
mongo_database_password = os.getenv('MONGO_DB_PASSWORD')

llm = Llama.from_pretrained(
    repo_id="bartowski/llama-3-fantasy-writer-8b-GGUF",
    filename="llama-3-fantasy-writer-8b-Q6_K.gguf",
    local_dir=hf_model_file
)

# Load the model and tokenizer
# pipe = pipeline("text-generation", model="imperialwool/ai-dungeon-large-en")

# Configure Kafka Consumer
kafka_consumer = Consumer({
    'bootstrap.servers': kafka_bootstrap_server,
    'group.id': 'item-processing-group',
    'auto.offset.reset': 'earliest'
})

kafka_consumer.subscribe([kafka_topic_name])

# Configure MongoDB Connection
mongo_client = MongoClient("mongodb://" + mongo_database_user + ":" + mongo_database_password +
                           "@" +
                           "localhost:" + mongo_database_port +
                           "/?authMechanism=SCRAM-SHA-1")
db = mongo_client[mongo_database_name]
collection = db['items']

def update_mongo_item(chat_id, item_id, generated_name):
    try:
        logging.debug(f"Updating MongoDB item with chatId: {chat_id}, id: {item_id}, name: {generated_name}")
        
        # Find the document by chatId and id, and update the 'name' field
        result = collection.update_one(
            {'chatId': chat_id, '_id': ObjectId(item_id)},  # Query to find the document
            {'$set': {'name': generated_name}}   # Update operation to set the 'name' field
        )

        logging.debug(result)
        
        if result.matched_count > 0:
            logging.debug("MongoDB item updated successfully")
        else:
            logging.warning(f"No MongoDB document updated for chatId: {chat_id}, id: {item_id}")
    except Exception as e:
        logging.error(f"Error updating MongoDB: {str(e)}")

def process_kafka_message(message):
    try:
        # Deserialize the Kafka message to a Python dictionary
        item_data = json.loads(message)
        chat_id = item_data['chatId']
        item_id = item_data['id']
        prompt = item_data['prompt']
    except Exception as e:
        logging.error(f"Error processing Kafka message: {str(e)}")

    logging.debug(f"Processing item with chatId: {chat_id}, id: {item_id}, prompt: {prompt}")

    # Run text-generation pipeline with the provided prompt
    response = llm.create_chat_completion(
        messages = [
            { 
                "role": "system", 
                "content": "Generate short (1-3 words) name for item from fantasy dungeon crawler rpg by given "
                           "description. Respond with one line of text containing item name, without formatting, "
                           "quotation marks, dots, or additional text"},
            {
                "role": "user",
                "content": prompt
            }
        ],
        temperature = 0.8,
        max_tokens = 10
    )
    try:
        logging.debug(f"Raw response from LLM: {response}")
        generated_name = json.loads(json.dumps(response))['choices'][0]['message']['content']

        logging.debug(f"Response content: {response}")
        # response = pipe(prompt, max_new_tokens=10)[0]['generated_text']
        
        logging.debug(f"Generated name: {generated_name}")
    except Exception as e:
        logging.error(f"Error processing LLM response: {str(e)}")
        # Update MongoDB with the generated name where chatId and id match
    update_mongo_item(chat_id, item_id, generated_name)
   

def consume_and_process_items():
    while True:
        msg = kafka_consumer.poll(1.0)  # Poll for new messages from Kafka
        if msg is None:
            continue
        if msg.error():
            logging.error(f"Kafka error: {msg.error()}")
            continue
        
        logging.debug(f"Received message from Kafka: {msg}")
        
        # Process the Kafka message
        process_kafka_message(msg.value().decode('utf-8'))

if __name__ == '__main__':
    # Start Kafka consumer in parallel
    consume_and_process_items()
