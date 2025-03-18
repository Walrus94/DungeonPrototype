import logging
import json
import os
from config.settings import HF_MODEL_FILE, HF_API_KEY, IMAGE_PATH;
from huggingface_hub import IntereferenceClient
from db.mongo import update_mongo_item
from llama_cpp import Llama

client = InferenceClient(
    provider="hf-inference",
    api_key=HF_API_KEY,
)

llm = Llama.from_pretrained(
    repo_id="bartowski/llama-3-fantasy-writer-8b-GGUF",
    filename="llama-3-fantasy-writer-8b-Q6_K.gguf",
    local_dir=HF_MODEL_FILE
)

def process_kafka_item_message(message):
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
            }r
        ],
        temperature = 0.8,
        max_tokens = 10
    )
    try:
        logging.debug(f"Raw response from LLM: {response}")
        generated_name = json.loads(json.dumps(response))['choices'][0]['message']['content']

        logging.debug(f"Response content: {response}")
        # response = pipe(prompt, max_new_tokens=10)[0]['generated_text']
        os.makedirs(IMAGE_PATH, exist_ok=True)
        
        logging.debug(f"Generated name: {generated_name}")
        image = client.text_to_image(
            generated_name+ ": " + prompt,
            model="proximasanfinetuning/fantassified_icons_v2"
        )

        image.save(f"{IMAGE_PATH}/{chat_id}_{item_id}.png")

    except Exception as e:
        logging.error(f"Error processing LLM response: {str(e)}")
        # Update MongoDB with the generated name where chatId and id match
    update_mongo_item(chat_id, item_id, generated_name)