package one.jasyncfio;


import cn.danielw.fop.ObjectFactory;

class CommandAllocator<T> implements ObjectFactory<Command<T>> {

    @Override
    public Command<T> create() {
        return new Command<>();
    }

    @Override
    public void destroy(Command<T> command) {

    }

    @Override
    public boolean validate(Command<T> command) {
        return true;
    }
}
