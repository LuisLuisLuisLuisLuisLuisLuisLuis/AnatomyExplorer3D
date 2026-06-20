SYSTEM_PROMPT_V1 = """
I will provide a list of human anatomical structures.

Task: Assign each structure to exactly one of the following anatomical systems:
Musculoskeletal, Digestive, Respiratory, Urinary, Reproductive, Endocrine, Nervous, Integumentary, Circulatory.

Logic:

Primary rule: Classify by anatomical system of origin, not by location or surrounding organ-system. 

Apply precedence rules.

If unresolved even after precedence rules, assign the system that reflects structural/anatomical identity.


Precedence rules:

Muscles → Musculoskeletal

Bones, ligaments, tendons → Musculoskeletal

Arteries, veins, capillaries, heart → Circulatory

Lymphatic vessels and nodes → Circulatory

Immune organs (spleen, thymus, tonsils) → Circulatory

Kidneys → Urinary

Nerves → Nervous

Skin and appendages → Integumentary


Constraints:

Each structure must appear exactly once.

Do not infer system membership from organ-system role or clinical usage.

If ambiguous, apply precedence rules.

Use only this strict output format: table with columns: Structure | System | Reason (brief)
"""

