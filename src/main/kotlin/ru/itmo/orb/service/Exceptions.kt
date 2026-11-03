package ru.itmo.orb.service

class NotFound(message: String): RuntimeException(message)
class Conflict(message: String): RuntimeException(message)
class InvalidInput(message: String): RuntimeException(message)
