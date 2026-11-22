// Safety page content. General public-safety guidance written for SIVA Crackers.
// Kept out of the page component so it can later be edited from the admin app.
// Emergency numbers are India's public helplines (112 national, 101 fire, 108 ambulance).

export const SAFETY_SECTIONS = [
  {
    id: 'before',
    icon: 'ClipboardCheck',
    title: 'Before you begin',
    points: [
      'Buy only from licensed shops and check that every pack shows the maker’s name and instructions.',
      'Read the instructions printed on each item before use.',
      'Store crackers in a cool, dry place in a closed box, away from heat, lamps, stoves and children.',
      'Choose a wide, open area away from buildings, vehicles, dry grass, electric wires and anything that can catch fire.',
      'Keep two buckets ready – one of water and one of sand – before lighting anything.',
    ],
  },
  {
    id: 'supervision',
    icon: 'Users',
    title: 'Adult supervision',
    points: [
      'An adult must be present and in charge at all times. Never leave children alone with crackers.',
      'Only adults should light larger items such as rockets, bombs, aerial shots and garlands.',
      'Even sparklers and kids’ items need close adult supervision.',
      'Keep spectators, especially children, the elderly and pets, at a safe distance behind the person lighting.',
      'Do not use crackers after drinking alcohol.',
    ],
  },
  {
    id: 'lighting',
    icon: 'Flame',
    title: 'Safe lighting practices',
    points: [
      'Use a long agarbatti (incense stick) or a long candle-lighter – never a matchstick held close.',
      'Light at arm’s length, from the side, and never lean over a cracker.',
      'Light one item at a time and step back immediately.',
      'Never light crackers in your hand, in a container, in pockets, or inside the house.',
      'Never point or throw crackers at people, animals, vehicles or houses.',
      'Place ground items on flat, firm ground; launch rockets only from a tube or bottle pointing straight up.',
    ],
  },
  {
    id: 'after',
    icon: 'Droplets',
    title: 'After lighting and misfires',
    points: [
      'If an item does not go off, do not go near it for at least 5 minutes. Never try to relight it.',
      'Pour water over misfires and used items, then dispose of them safely.',
      'Put used sparklers and pencils into a bucket of water – the wire stays hot for minutes.',
      'Do not pick up partly burnt items or let children collect them.',
    ],
  },
  {
    id: 'protection',
    icon: 'Shirt',
    title: 'Clothing and protection',
    points: [
      'Wear close-fitting cotton clothes. Avoid loose, flowing or synthetic clothing such as nylon.',
      'Wear proper footwear – never be barefoot.',
      'Tie back long hair and keep dupattas and sarees away from sparks.',
      'Protect ears from loud crackers, especially for children and the elderly.',
    ],
  },
  {
    id: 'others',
    icon: 'HeartHandshake',
    title: 'Care for others and the law',
    points: [
      'Follow the timings, permitted areas and noise rules announced by your local authorities.',
      'Avoid bursting crackers near hospitals, schools, places of worship and old-age homes.',
      'Keep pets indoors in a quiet room.',
      'Clean up after celebrating and dispose of waste safely.',
    ],
  },
];

export const EMERGENCY = {
  title: 'Emergency precautions',
  firstAid: [
    {
      title: 'Burns',
      text: 'Cool the burn under clean running water for 10–20 minutes. Do not apply ice, butter, toothpaste or ointments. Remove rings or tight items near the burn and cover loosely with a clean cloth. Seek medical help for anything larger than a small coin, or on the face, hands or joints.',
    },
    {
      title: 'Eye injuries',
      text: 'Do not rub the eye or try to remove anything stuck in it. For dust or sparks, gently rinse with clean water. Cover the eye lightly and go to a hospital immediately.',
    },
    {
      title: 'Clothes on fire',
      text: 'Stop, drop and roll: stop moving, drop to the ground, cover your face and roll to put out the flames. Or wrap the person in a thick blanket. Never run.',
    },
    {
      title: 'Fire',
      text: 'Move everyone away and call the fire service. Use water or sand only on small fires and only if it is safe to do so.',
    },
  ],
  note: 'These first-aid steps are general guidance, not medical advice. For any serious injury, call 108 or 112 immediately and get medical help.',
  numbers: [
    { label: 'National emergency', number: '112' },
    { label: 'Fire service', number: '101' },
    { label: 'Ambulance', number: '108' },
  ],
};

export const SAFETY_DISCLAIMER =
  'This guidance is general information to help you celebrate safely. It is not a complete list of every risk and does not replace the instructions printed on each product or the rules set by local authorities. Always follow the manufacturer’s instructions and applicable laws. Use of fireworks is at the user’s own risk and responsibility.';
