from Perplexity import Main
import prompts

with open("XXXX") as file:
    with open("XXXX", "w", encoding="utf-8", errors="replace") as resultFile:
        counter = 0
        split = ""
        lines = file.readlines()
        for line in lines:
            split += line
            counter += 1
            if counter == 175:
                resultFile.write(str(Main.main(split, prompts.SYSTEM_PROMPT_V1).choices[0].message.content))
                counter = 0
                split=""
                pass
            pass
        if split != "":
            resultFile.write(str(Main.main(split, prompts.SYSTEM_PROMPT_V1).choices[0].message.content))
            pass


