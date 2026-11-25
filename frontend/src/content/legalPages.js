// Legal page drafts for /legal/:slug. Written for an information-only catalogue.
// DRAFT: to be reviewed by the shop owner and a legal adviser before going live.

export const LEGAL_REVIEW_NOTE = 'Draft text – to be reviewed by SIVA Crackers and a legal adviser before publication.';

export const LEGAL_PAGES = {
  disclaimer: {
    title: 'Legal Disclaimer',
    intro: 'Please read this disclaimer before using the SIVA Crackers website.',
    sections: [
      {
        heading: 'Information catalogue only',
        body: [
          'This website is an information catalogue. It shows products, indicative prices and general information about SIVA Crackers, Eachanari, Coimbatore.',
          'SIVA Crackers does not sell, book, reserve, accept payment for, or deliver firecrackers through this website, by WhatsApp, or by any other online means. All purchases are made in person at the shop, subject to applicable laws and local regulations.',
        ],
      },
      {
        heading: 'Prices and availability',
        body: [
          'All prices shown are indicative only and may change without notice. MRP is the maximum retail price printed on the pack. The final price, availability and any offers are confirmed only at the shop.',
          'Stock levels shown on the website may not be up to date.',
        ],
      },
      {
        heading: 'Demonstration data',
        body: [
          'While the website is being developed, products, brands, prices and stock levels marked “DEMO” are sample data and do not represent the shop’s actual range.',
        ],
      },
      {
        heading: 'Enquiries',
        body: [
          'Any WhatsApp, phone or email contact is for general product enquiries only. No order, booking or payment is accepted through these channels.',
        ],
      },
      {
        heading: 'Safety and responsibility',
        body: [
          'Fireworks are hazardous products. Safety information on this website is general guidance only. Always follow the instructions printed on each product and the rules set by local authorities. Use of fireworks is at the user’s own risk.',
        ],
      },
      {
        heading: 'Images and illustrations',
        body: ['Product images may be illustrations and the actual product and packaging may differ.'],
      },
    ],
  },
  privacy: {
    title: 'Privacy Policy',
    intro: 'This policy explains what information the SIVA Crackers website uses and why.',
    sections: [
      {
        heading: 'Information stored on your device',
        body: [
          'If you use “My Estimate List”, the products and quantities you choose are saved only in your own browser (local storage). They are never sent to SIVA Crackers or any server. You can clear the list at any time from the Estimate List page or by clearing your browser data.',
        ],
      },
      {
        heading: 'WhatsApp, phone and email',
        body: [
          'If you choose to contact us by WhatsApp, phone or email, the information you send (such as your name, number and message) is handled through that service and used only to answer your enquiry.',
        ],
      },
      {
        heading: 'Customer accounts',
        body: [
          'If you create a customer account, we store your name, email address, optional mobile number, and the date your account was created and last used. Your password is never stored as written: only a one-way encrypted form (BCrypt) is kept, so no one at the shop can see it.',
          'When you log in, the website sets a secure login cookie that your browser sends back to our server. JavaScript on the page cannot read it, it is used only to keep you logged in, and it expires after 60 minutes or when you log out. A second, separate cookie protects the forms on this website against misuse.',
          'Account information is used only to provide your account. No orders, bookings or payments are taken online. You can update your details from My Account; to have your account deleted, please contact the shop using the details on the Contact Us page.',
        ],
      },
      {
        heading: 'Third-party content',
        body: [
          'The contact page shows a Google Maps view of the Eachanari area. Loading the map is subject to Google’s own privacy policy. Fonts are loaded from Google Fonts.',
        ],
      },
      {
        heading: 'Contact',
        body: ['For privacy questions, please contact the shop using the details on the Contact Us page.'],
      },
    ],
  },
  terms: {
    title: 'Terms of Use',
    intro: 'By using this website you agree to these terms.',
    sections: [
      {
        heading: 'Purpose of the website',
        body: [
          'The website provides information about the products sold at the SIVA Crackers shop in Eachanari, Coimbatore. It is not an online shop and does not form any contract of sale.',
        ],
      },
      {
        heading: 'No online sale of firecrackers',
        body: [
          'Firecrackers are not sold, booked, reserved, paid for or delivered through this website or any related messaging service. Nothing on the website, including the Estimate List, is an offer to sell or an order.',
        ],
      },
      {
        heading: 'Accuracy',
        body: [
          'We try to keep information accurate, but descriptions, images, prices and availability may contain errors or change without notice. Please confirm all details at the shop.',
        ],
      },
      {
        heading: 'Acceptable use',
        body: ['Do not misuse the website, attempt to disrupt it, or copy its content or design for commercial use without permission.'],
      },
      {
        heading: 'Brand and content',
        body: ['The SIVA Crackers name, logo, illustrations and website design are the property of SIVA Crackers.'],
      },
      {
        heading: 'Changes',
        body: ['These terms may be updated from time to time. The version on this page applies.'],
      },
    ],
  },
};
