from item import Item
import errors


class ShoppingCart:
    # Initialize
    def __init__(self) -> None:
        self.cart_items = []

    def add_item(self, item: Item):
         # if the item isn't already in the shopping cart add it
        if(item not in self.cart_items):
            self.cart_items.append(item)
         #if the item already exist raise error
        else:
            raise errors.ItemAlreadyExistsError
  
    def remove_item(self, item_name: str):
        found = False
        for item in self.cart_items:
            # if the item is in the shopping cart remove it
            if item.name == item_name:
                self.cart_items.remove(item)
                found = True
                break
        #if the item not exist raise error
        if not found:
            raise errors.ItemNotExistError

    def get_subtotal(self) -> int:
        res = 0
        #adding the price of each item
        for item in self.cart_items:
            res += item.price
        return res
       
        
