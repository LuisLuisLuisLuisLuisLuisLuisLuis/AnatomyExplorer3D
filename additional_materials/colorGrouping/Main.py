from perplexity import Perplexity, Stream
from perplexity.types import StreamChunk
import os


def main(message:str, system_prompt:str) -> StreamChunk | Stream[StreamChunk]:
    perplexity = Perplexity(api_key=os.environ["XXX"])

    result = perplexity.chat.completions.create(
        messages=[
            {
                "role" : "system",
                "content" : system_prompt
            },
            {
                "role" : "user",
                "content" : message,
            }
        ],
        model="sonar-pro",
        temperature=0,
    )
    print("-----------to dict---------------")
    print(result.to_dict())
    print("------------------")
    print(f"Response: {result.choices[0].message.content}")
    return result.choices[0].message.content
