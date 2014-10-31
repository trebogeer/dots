set nocompatible              " be iMproved, required
filetype off                  " required

" set the runtime path to include Vundle and initialize
set rtp+=~/.vim/bundle/Vundle.vim
call vundle#begin()
" alternatively, pass a path where Vundle should install plugins
"call vundle#begin('~/some/path/here')

" let Vundle manage Vundle, required
Plugin 'gmarik/Vundle.vim'
Plugin 'tpope/vim-fugitive'
Plugin 'Valloric/YouCompleteMe'
Plugin 'fatih/vim-go' 
Plugin 'kien/ctrlp.vim'
" The following are examples of different formats supported.
" Keep Plugin commands between vundle#begin/end.
" plugin on GitHub repo
"Plugin 'tpope/vim-fugitive'
" plugin from http://vim-scripts.org/vim/scripts.html
"Plugin 'L9'
" Git plugin not hosted on GitHub
"Plugin 'git://git.wincent.com/command-t.git'
" git repos on your local machine (i.e. when working on your own plugin)
"Plugin 'file:///home/gmarik/path/to/plugin'
" The sparkup vim script is in a subdirectory of this repo called vim.
" Pass the path to set the runtimepath properly.
"Plugin 'rstacruz/sparkup', {'rtp': 'vim/'}
" Avoid a name conflict with L9
"Plugin 'user/L9', {'name': 'newL9'}

" All of your Plugins must be added before the following line
call vundle#end()            " required
filetype plugin indent on    " required
" To ignore plugin indent changes, instead use:
"filetype plugin on
"
" Brief help
" :PluginList       - lists configured plugins
" :PluginInstall    - installs plugins; append `!` to update or just :PluginUpdate
" :PluginSearch foo - searches for foo; append `!` to refresh local cache
" :PluginClean      - confirms removal of unused plugins; append `!` to auto-approve removal
"
" see :h vundle for more details or wiki for FAQ
" Put your non-Plugin stuff after this line
"
set noerrorbells  "No beeps
set number        "Show line numbers
set backspace=indent,eol,start  "More powerful backspace
set showcmd       "Show typing
set showmode      "Show mode

set noswapfile    "Don't use swap
set nobackup      "Don't use backup files
set splitright    "Split vertical windows right to the current window
set splitbelow    "Split horizontal windows below current
set encoding=utf-8 "Set default encoding to utf-8
set autowrite     "Auto save bafore make, next, etc
set autoread      "Auto read changed files
set fileformats=unix,dos,mac


"set noshowmatch   "Do not show matching brackets by flickering
set expandtab      "convert tabs into spaces
set smarttab       "Be smart!
set tabstop=4      "tab = 8 spaces
set shiftwidth=4   "indentation is 4 spaces
set softtabstop=4  "4 spaces 

set lazyredraw     "Wait to redraw
set incsearch      "show the match while typing
set hlsearch       "Highlight found searches
set ignorecase     "case insensitive search
set smartcase      "not when search contains upper case chars
set ttyfast

"speed up highlighting
set nocursorcolumn
set nocursorline
syntax sync minlines=256
set synmaxcol=128
set re=1

set statusline=%<%f\ %h%m%r%{fugitive#statusline()}%=%-14.(%l,%c%V%)\ %P

