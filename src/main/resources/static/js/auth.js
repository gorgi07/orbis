'use strict';
document.querySelectorAll('[data-toggle-password]').forEach(button => {
  button.addEventListener('click', () => {
    const input = document.getElementById(button.dataset.togglePassword);
    if (!input) return;
    const visible = input.type === 'password';
    input.type = visible ? 'text' : 'password';
    button.textContent = visible ? 'Скрыть' : 'Показать';
    button.setAttribute('aria-label', visible ? 'Скрыть пароль' : 'Показать пароль');
  });
});
