// Sample FAQ data
const faqData = [
    {
        question: "What is the purpose of activating the 'File name encryption/Folder name encryption' checkbox?",
        answer: "When you choose to enable the 'File name encryption' checkbox in FilesCrypt Pro, it goes beyond merely encrypting the file or folder contents; it also encrypts the names of all the files for increased security. Similarly, when you opt for the 'Folder name encryption' checkbox, FilesCrypt Pro encrypts the names of all the folders, adding an extra layer of security to your data."
    },
    {
        question: "What function does enabling the 'Delete source files upon successful encryption/decryption' checkbox do?",
        answer: "Enabling the 'Delete source files upon successful encryption' option in FilesCrypt Pro results in the automatic deletion of the original file after it has been successfully encrypted. This ensures that only the encrypted version of the file remains. Similarly, when you enable the 'Delete source files upon successful decryption' option, FilesCrypt Pro deletes the original encrypted file after successfully decrypting it, leaving only the decrypted version behind."
    },
    {
        question: "How reliable is it to store passwords in a password manager?",
        answer: "The password manager securely stores passwords in a way that only FilesCrypt Pro can access them. Even if your device is rooted, it remains challenging for hackers to obtain the keystore because it is protected by a password."
    },
    {
        question: "How to decrypt a file or folder using only your fingerprint or your device's screen lock?",
        answer: "To decrypt your files using only your fingerprint or your device's screen lock, you need to follow these steps:\n\n1. Make sure you have saved the password used for encrypting the files or folders by enabling the 'Save this password to Password Manager' checkbox, if the password is already saved in Password Manager you can ignore this checkbox.\n\n2. After selecting the files or folder you want to decrypt, click the 'Retrieve password from Password Manager' button.\n\nBy doing this, you can access the necessary password securely stored in the Password Manager, allowing you to decrypt your files with the added convenience of biometric or screen lock authentication."
    },
    {
        question: "Is it possible for FilesCrypt Pro to decrypt files that have been encrypted by other applications?",
        answer: "If other encrypted files from different applications store their encryption parameters in the same way as FilesCrypt Pro does, specifically at the beginning of the file in JSON format, then it is possible to decrypt those files using FilesCrypt Pro. This compatibility in parameter storage allows for seamless decryption across files with matching encryption settings, regardless of the application used for encryption."
    },
    {
        question: "Let's discuss about the initialization vector in FilesCrypt Pro.",
        answer: "In FilesCrypt Pro, the initialization vector (IV) provided by the user or automatically generated for each file is submitted to the application in byte format encoded using UTF-8 format. However, when encrypting folder names, a constant initialization vector of '0000000000000000' is employed exclusively for folders."
    }
];

const searchInput = document.getElementById('searchInput');
const faqList = document.getElementById('faqList');

// Function to highlight keywords within text
function highlightKeywords(text, keywords) {
    const regex = new RegExp(`(${keywords.join('|')})`, 'gi');
    return text.replace(regex, '<span class="highlighted">$1</span>');
}

// Function to display FAQ questions and answers
function displayFAQ(query) {
    faqList.innerHTML = '';

    faqData.forEach(item => {
        const questionMatch = item.question.toLowerCase().includes(query.toLowerCase());
        const answerMatch = item.answer.toLowerCase().includes(query.toLowerCase());

        if (questionMatch || answerMatch) {
            const li = document.createElement('li');
            const highlightedQuestion = highlightKeywords(item.question, [query]);
            const highlightedAnswer = highlightKeywords(item.answer, [query]);
            li.innerHTML = `<strong>${highlightedQuestion}</strong><br>${highlightedAnswer}`;
            faqList.appendChild(li);
        }
    });
}

// Initial display of FAQ questions and answers
displayFAQ('');

// Event listener for search input
searchInput.addEventListener('input', function() {
    displayFAQ(this.value);
});
