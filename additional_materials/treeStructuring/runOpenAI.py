from openai import OpenAI
import systemPrompt
import os


def run(system_prompt:str, message:str, model:str = "gpt-5.1", temperature:float = 0.0) -> str:

    client = OpenAI(api_key=os.environ['XXXX'])

    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": message},
        ],
        temperature=temperature,
    )
    print(response.to_dict())

    return response.choices[0].message.content


def main():

    with open(FILENAME, "w", encoding="utf-8", errors="replace") as resultFile:
        resultFile.write(str(run(systemPrompt.SYS_SKULL, MESSAGE)))
        pass
    pass

FILENAME = "skull03.txt"

MESSAGE = """
Here is the tree of the skull: 

FMA46565	skull	FMA53672	neurocranium
FMA46565	skull	FMA53673	viscerocranium
FMA46565	skull	newnode9039124	left major alar cartilage
FMA46565	skull	newnode338841	right lateral nasal cartilage
FMA46565	skull	newnode9318511	left lateral nasal cartilage
FMA46565	skull	newnode2788260	right major alar cartilage
FMA46565	skull	FMA49187	occipital bone
FMA53672	neurocranium	FMA52734	frontal bone
FMA53672	neurocranium	FMA52788	right parietal bone
FMA53672	neurocranium	FMA52789	left parietal bone
FMA53672	neurocranium	FMA52735	occipital bone
FMA53672	neurocranium	FMA52736	sphenoid bone
FMA53672	neurocranium	FMA52738	right temporal bone
FMA53672	neurocranium	FMA52739	left temporal bone
FMA53672	neurocranium	FMA52801	basicranium
FMA53673	viscerocranium	FMA53649	right maxilla
FMA53673	viscerocranium	FMA53650	left maxilla
FMA53673	viscerocranium	FMA52892	right zygomatic bone
FMA53673	viscerocranium	FMA52893	left zygomatic bone
FMA53673	viscerocranium	FMA53645	right lacrimal bone
FMA53673	viscerocranium	FMA53646	left lacrimal bone
FMA53673	viscerocranium	FMA53655	right palatine bone
FMA53673	viscerocranium	FMA53656	left palatine bone
FMA53673	viscerocranium	FMA54737	right inferior nasal concha
FMA53673	viscerocranium	FMA54738	left inferior nasal concha
FMA53673	viscerocranium	FMA9710	vomer
FMA53673	viscerocranium	FMA52740	ethmoid
FMA53673	viscerocranium	FMA52748	mandible
FMA53673	viscerocranium	FMA52749	hyoid bone
FMA53673	viscerocranium	FMA59654	osseous skeleton of nose
FMA53673	viscerocranium	FMA54398	lower jaw
FMA53673	viscerocranium	FMA54397	upper jaw
FMA53673	viscerocranium	FMA53637	right cheek
FMA53673	viscerocranium	FMA53638	left cheek
FMA53673	viscerocranium	FMA61670	skeleton of mouth
FMA53673	viscerocranium	FMA46472	nose
FMA59654	osseous skeleton of nose	FMA59655	osseous skeleton of external nose
FMA59654	osseous skeleton of nose	FMA59656	osseous skeleton of internal nose
FMA59655	osseous skeleton of external nose	FMA53647	right nasal bone
FMA59655	osseous skeleton of external nose	FMA53648	left nasal bone
FMA46472	nose	FMA60116	nasal skeleton
FMA46472	nose	FMA59515	external nose
FMA46472	nose	FMA54375	nasal septum
FMA46472	nose	FMA59637	internal nose
FMA60116	nasal skeleton	FMA59538	cartilaginous skeleton of nose
FMA59515	external nose	FMA59516	root of nose
FMA59515	external nose	FMA59517	dorsum of nose
FMA59515	external nose	FMA59836	cartilaginous skeleton of external nose
FMA59836	cartilaginous skeleton of external nose	FMA59505	right major alar cartilage
FMA59836	cartilaginous skeleton of external nose	FMA59506	left major alar cartilage
FMA59836	cartilaginous skeleton of external nose	FMA59512	right lateral nasal cartilage
FMA59836	cartilaginous skeleton of external nose	FMA59513	left lateral nasal cartilage
FMA59836	cartilaginous skeleton of external nose	FMA59503	septal nasal cartilage
FMA54375	nasal septum	FMA60118	skeleton of nasal septum
FMA60118	skeleton of nasal septum	FMA59837	bony part of nasal septum
FMA60118	skeleton of nasal septum	FMA59838	cartilaginous part of nasal septum
FMA59637	internal nose	FMA59748	right side of internal nose
FMA59637	internal nose	FMA59749	left side of internal nose
FMA59637	internal nose	FMA59668	wall of internal nose
FMA59748	right side of internal nose	FMA59754	wall of right side of internal nose
FMA59749	left side of internal nose	FMA59755	wall of left side of internal nose
FMA59755	wall of left side of internal nose	FMA59672	left lateral wall of internal nose
FMA59755	wall of left side of internal nose	FMA59665	septum of internal nose
FMA59668	wall of internal nose	FMA59671	right lateral wall of internal nose
"""

if __name__ == "__main__":
    main()
    pass