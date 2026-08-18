# Chat App

A modern Android chat application built with Kotlin and designed with a clean MVVM architecture.

## Features

- User registration and sign-in
- Firebase Authentication
- User profiles
- User and Manager roles
- Friend management
- Real-time chat functionality
- User management
- Edit and delete users
- Multiple-user selection and bulk deletion
- Password visibility toggle
- Custom reusable input components
- Modern dark UI
- RecyclerView-based lists
- ViewBinding
- Kotlin Coroutines and StateFlow

## Architecture

The application follows the MVVM (Model-View-ViewModel) architecture.

```text
UI
│
├── Activities
├── RecyclerView Adapters
└── Custom Views
        │
        ▼
    ViewModel
        │
        ▼
    Repository
        │
        ▼
     Firebase
